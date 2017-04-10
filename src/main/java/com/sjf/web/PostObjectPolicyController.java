package com.sjf.web;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import net.sf.json.JSONObject;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by SJF on 2017/2/21.
 */
@Controller
@EnableAsync
public class PostObjectPolicyController extends HttpServlet {

    private static final long serialVersionUID = 5522372203700422672L;

    @RequestMapping("/oss/post")
    public void postObject(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        String accessId = "YOUR ACCESSID";
        String accessKey = "YOUR ACCESSKEY";
        String bucket = "YOUR BUCKET";
        String dir = "YOUR DIR";
        String host = "http://" + bucket + "." + endpoint;

        Map<String, Object> callMap = new LinkedHashMap<>();
        callMap.put("callbackUrl", "http://154079m3d2.iok.la:16052/oss/callback");
        callMap.put("callbackHost", "154079m3d2.iok.la");
        //callMap.put("callbackBody", "filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width}");
        callMap.put("callbackBody", "${object}&${size}&${mimeType}&${imageInfo.height}&${imageInfo.width}");
        callMap.put("callbackBodyType", "application/json");
        JSONObject callbackJson = JSONObject.fromObject(callMap);
        String callback = BinaryUtil.toBase64String(callbackJson.toString().getBytes("utf-8"));

        OSSClient client = new OSSClient(endpoint, accessId, accessKey);

        long expireTime = 30;
        long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
        Date expiration = new Date(expireEndTime);
        PolicyConditions policyConds = new PolicyConditions();
        policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
        policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

        String postPolicy = client.generatePostPolicy(expiration, policyConds);
        byte[] binaryData = postPolicy.getBytes("utf-8");
        String encodedPolicy = BinaryUtil.toBase64String(binaryData);
        String postSignature = client.calculatePostSignature(postPolicy);

        Map<String, String> respMap = new LinkedHashMap<>();
        respMap.put("accessid", accessId);
        respMap.put("policy", encodedPolicy);
        respMap.put("signature", postSignature);
        respMap.put("dir", dir);
        respMap.put("host", host);
        respMap.put("expire", String.valueOf(expireEndTime / 1000));
        respMap.put("callback", callback);
        JSONObject ja1 = JSONObject.fromObject(respMap);
        System.out.println(ja1.toString());
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST");
        response(request, response, ja1.toString());
    }

    private void response(HttpServletRequest request, HttpServletResponse response, String results) throws IOException {
        String callbackFunName = request.getParameter("callback");
        if (callbackFunName==null || callbackFunName.equalsIgnoreCase(""))
            response.getWriter().println(results);
        else
            response.getWriter().println(callbackFunName + "( "+results+" )");
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
    }

    @RequestMapping("/")
    public String getIndex() {
        return "index";
    }

}

package com.naver.www.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;    
import javax.servlet.http.HttpServletResponse;   
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller 
public class MainController {

    @Autowired
    KakaoService kakaoService;
    // @RequestMapping("test")
    // @ResponseBody
    // public String testConnect() {
    //     return "연결성공";
    // }
    @RequestMapping("/kakao/sign_in")
    public String kakaoSignIn(@RequestParam(value = "code") String code) throws Exception {
    	System.out.println(code);
        Map<String,Object> result = kakaoService.execKakaoLogin(code);
        return "redirect:webauthcallback://success?accessToken="+result.get("accessToken")+"&customToken="+result.get("customToken").toString();
    }

    @RequestMapping("/kakao/sign_out")
    public void kakaoSignOut(@RequestParam(value = "code") String code) {
        kakaoService.kakaoLogout(code);
    }
} 
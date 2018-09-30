package com.example.xxx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@ResponseBody
public class HomeController {
    @RequestMapping("/aaa/index")
    public String index(Model model) {
        return "aaa";
    }
    @RequestMapping(value="/user/login")
    public String login(Model model) {
        return "login";
    }
    @RequestMapping("/")
    public String root(Model model) {
        return "index";
    }
    
////    @PreAuthorize("hasAuthority('ROLE_USER')")
//    @RequestMapping(value="/admin/test",method = RequestMethod.POST)
//    @ResponseBody
//    public String adminTest1() {
//        return "ROLE_USER";
//    }
//    
////    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    @RequestMapping("/auto/test")
//    @ResponseBody
//    public String adminTest2() {
//        return "ROLE_ADMIN";
//    }
}

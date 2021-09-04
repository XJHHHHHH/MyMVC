package controller;

import mvc.RequestMapping;
import mvc.ResponseBody;


@RequestMapping("ShoppingController.do")
public class ShoppingController{

    @RequestMapping("buy.do")
    public String buy() {
        System.out.println("这是的ShoppingController中的buy方法");
        return "welcome.jsp";
    }

    @RequestMapping("find.do")
    @ResponseBody
    public String find() {
        System.out.println("这是的ShoppingController中的find方法");
        return "xxx";
    }

}

package controller;

import domain.User;
import mvc.ModelAndView;
import mvc.RequestMapping;
import mvc.ResponseBody;
import mvc.SessionAttributes;
import service.AtmService;





/**让每个Controller中可以做多件事，而不是单纯的一个功能请求
 * 松耦合
 *        现在不用继承父类，方法不用重写 doPost doGet service
 *        参数可以随意 可以使String int float （不能是接口，数组，List、Set不行）
 *        现在方法没有异常
 *        返回值可以是 String（viewName,数据） modelAndView  domain对象 还可以是集合
 *        管理机制 底层延迟加载 单列机制
 * */

@SessionAttributes("name")
public class ATMController {
    private AtmService service = new AtmService();

    @RequestMapping("login.do")
    public ModelAndView login(User user){
        ModelAndView mv = new ModelAndView();
        System.out.println("这是ATMController中的login方法");
        System.out.println(user.getName()+"--"+user.getPass());
        String result =service.login(user);
        if("success".equals(result)){
            mv.setViewName("welcome.jsp");
            mv.addAttributeObject("name",user.getName());
        }else {
            mv.setViewName("index.jsp");
            mv.addAttributeObject("result",result);
        }
        return mv;
    }

    @RequestMapping("query.do")
    @ResponseBody
    public String query(User user){
        System.out.println("这是ATMController中的query方法");
        System.out.println(user);
        return "xxx";
        //返回值返回执行完后要转发的资源名
    }
}

package mvc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 这是所有的controller的总管
   控制所有的请求找到的方式
 */
public class DispatcherServlet extends HttpServlet {

    /**用一个Handler类当属性,调用里面的方法*/
    private Handler handler = new Handler();

    /**让这个总管类对象一创建就缓存properties文件*/
    @Override
    public void init(ServletConfig config){
        boolean flag = handler.loadPropertiesFile();
        String packageName = null;
       if(!flag){
           //如果能执行，则说明没有properties文件
           //需要去XMl文件里去找
           packageName = config.getInitParameter("scanPackage");
       }else {
           packageName = handler.getScanPackage();
       }
       //然后扫描注解
        handler.scanAnnotation(packageName);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //所有的请求都先找到这个总管，由他来分配请求到达的控制类和方法
        //1.解析请求的信息，获取请求要取的控制类
        //解析请求的统一资源标识符
        //得到请求的控制类名,和请求要执行的方法
        String uri = request.getRequestURI();
        String requestContent = handler.parseUriName(uri);
        String methodName =request.getParameter("method");
        //如果methodName==null,说明请求名就是方法名
        if(methodName==null){
            methodName = requestContent.substring(0,requestContent.indexOf("."));
        }
        //2.创建控制类的对象obj
        try {
            Object obj = handler.getObject(requestContent);
            //3.获取要执行的方法//3.找到控制类中请求要执行方法（IOC Inversion of Control）
            Method method = handler.findRealMethod(obj,methodName);
            //4.为要执行的方法注入所需要的参数（DI Dependency Injection）
            Object[] parameterValue = handler.injectionParameters(method,request,response);
            //5.执行方法,有一个返回值还需要做处理
            Object  result = method.invoke(obj,parameterValue);
            //6.做最后的结果处理
            handler.finalResolver(obj,method,result,request,response);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

    }
}

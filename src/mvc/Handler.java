package mvc;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

//这个类就是为了帮助总管DispatcherController类做事

public class Handler {

    /**用这个Map存储文件中类名和真实类名的对应关系，缓存机制*/
    private HashMap<String,String> realClassNameMap = new HashMap<>();
    //用这个Map存储控制类对象，懒加载机制
    private HashMap<String,Object> realObjectMap = new HashMap<>();
    /**属性，拥有个Map存储某一个Controller对象和它所有方法的对应关系*/
    private HashMap<Object,HashMap<String, Method>> objectMethodMap = new HashMap<>();
    /**这个Map存放 请求方法和类名的关系*/
    private HashMap<String,String> methodRealClassNameMap = new HashMap<>();

    String getScanPackage(){
        return this.realClassNameMap.get("scanPackage");
    }

    /**1.小弟一号，用来读取properties文件*/
    boolean loadPropertiesFile(){
        //创建一个boolean变量，如果properties文件不存在，boolean变为false
        boolean flag = true;
        try {
            Properties properties = new Properties();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ApplicationContext.properties");
            properties.load(inputStream);
            //获取所有的类名
            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()){
                String key = (String)enumeration.nextElement();
                String className = properties.getProperty(key);
                //将类名和真实3类名的对应关系存入缓存map realClassNameMap
                realClassNameMap.put(key,className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            flag=false;
        }
        return flag;
    }

    /**2.第二个小弟，解析请求uri，获取控制类全名*/
    String parseUriName(String uriName){
        return  uriName.substring(uriName.lastIndexOf("/")+1);

    }

    /**
     * 这个方法用于扫描方法或类上的注解
     * 得到请求名字
     * 扫描一个包下的所有类和方法,参数是一个包名
     * */
    void scanAnnotation(String packageNames){
        //如果packageNames==null说明用户没有配置要扫描的包名（用的是properties配置请求和类名的关系不是注解）
        if(packageNames==null){
            return;
        }
        //否则就存在包名
        //可能要扫描的包名有多个，先按照 ， 拆分
        String [] packages = packageNames.split(",");
        for(String packageName :packages){
            //循环获取要扫描的包名
            //获取包名中真实类文件的路径(存储地址) 加载到内存中扫描类中的注解
            //文件名\文件\文件.class
            //通过类加载器classLoader获取包文件对应的路径，获取文件的url 加载file类
            //将包名中的 . 替换为 \ 文件存储格式
            URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packageName.replace(".","\\"));
            if(packageUrl==null){
                continue;
            }
            //根据url获取一个文件的真实路径path
            String packagePath = packageUrl.getPath();
            //用packagePath创建一个File对象，操作硬盘上的对应文件
            File packageFile = new File(packagePath);
            //这个packageFile是包名文件夹，我们需要获取里面的子文件，
            //先做个过滤，只要后缀为 .class的文件
            File[] packageFiles = packageFile.listFiles(file -> {
                //λ函数用法
                if(file.isFile() && file.getName().endsWith("class")){
                    return true;
                }
                return false;
            });

            //操作包名文件夹下的子文件，遍历每一个File，通过放射加载类
            for(File file:packageFiles){
                //获取file文件的名字
                String fileName = file.getName();
                //然后拼接类全名
                String fileFullName = packageName+"."+fileName.substring(0,fileName.indexOf("."));
                //然后反射加载类
                try {
                    Class clazz = Class.forName(fileFullName);
                    //然后获取类上面注解
                    RequestMapping classRequestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);;
                    if(classRequestMapping!=null){
                        //如果不等于null，证明有注解,然后添加进realClassNameMap集合
                        realClassNameMap.put(classRequestMapping.value(),fileFullName);
                    }
                    //然后获取类中的方法,扫描方法上面是够有注解
                    //要获取方法就得先获取类
                    Method[] methods = clazz.getDeclaredMethods();
                    //遍历方法，看是否有注解
                    for(Method method:methods){
                        RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                        if(methodRequestMapping!=null){
                            methodRealClassNameMap.put(methodRequestMapping.value(),fileFullName);
                        }else {
                            //否则就抛出一个异常
                            //throw new NoSuchMethodException("没有找到对应的方法，请检查注解");
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**3.小弟三号，根据类全名获取类的对象*/
    Object getObject(String requestContent) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //先根据类全名从realObjectMap中取对象
        Object obj = realObjectMap.get(requestContent);
        //如果obj==null,证明这个对象没有被创建过
        if(obj==null) {
            String realClassName = realClassNameMap.get(requestContent);
            if (realClassName == null) {
                //如果realClassName==null，
                //就去method与类名集合去找
                realClassName = methodRealClassNameMap.get(requestContent);
                if(realClassName==null){
                    //证明请求名字有误,抛出一个异常
                    throw new ControllerNameNotFoundException(requestContent+"类全名没有找到，请检查");
                }
            }
            //否则创建这个类
            Class clazz = Class.forName(realClassName);
            try {
                obj = clazz.getDeclaredConstructor().newInstance();
            } catch (java.lang.NoSuchMethodException e) {
                e.printStackTrace();
            }
            realObjectMap.put(requestContent,obj);
            //获取当前类中的所有方法，和obj一起存入objectMethodMap
            Method[] methods = clazz.getMethods();
            HashMap<String,Method> methodMap = new HashMap<>();
            for(Method method:methods){
                methodMap.put(method.getName(),method);
            }
            objectMethodMap.put(obj,methodMap);
        }
        return obj;
    }

    /**4.小弟四号，根据类对象获取要执行的方法*/
    Method findRealMethod(Object obj,String methodName){
        HashMap<String,Method> methodMap = objectMethodMap.get(obj);
        return methodMap.get(methodName);
    }

    /**
     * 下面这些小弟是只为injectionParameters方法做事，
     * 判断参数类型是什么
     * */

    /**
     * 小弟五号，这个方法给要执行的方法注入所需要的对象
     * 返回一个动态数组，交给方法反射执行
     * 参数，要执行的方法，获取要执行的方法里面的参数
     *
     * */
    Object[] injectionParameters(Method method, HttpServletRequest request, HttpServletResponse response) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        //返回方法执行所需要的参数的值，
        //不需要对象自己去获取

        //获取方法的所有参数对象
        Parameter[] parameters = method.getParameters();
        if(parameters==null||parameters.length==0){
            //说明这个方法没有参数,直接返回null
            return null;
        }
        //如果有参数,创建一个Object数组，长度为方法所有参数的长度
        Object[] finalParameterValue = new Object[parameters.length];
        //解析参数的数据类型
        //如果有注解类型的，是一个一个的值，否者应该是一个完整类对象
        for(int i =0;i<parameters.length;i++) {
            //获取每一个参数
            Parameter parameter = parameters[i];
            //如果这个参数有注解，获取当前注解
            RequestParam paramAnnotation = parameter.getAnnotation(RequestParam.class);
            //如果这个注解不为null，获取注解里的值
            if (paramAnnotation != null) {
                String key = paramAnnotation.value();
                String value = request.getParameter(key);
                //如果value不为null，获取当前参数parameter的数据类型，然后装入finalParameterValue[]数组
                if (value != null) {
                    Class paramClazz = parameter.getType();
                    if (paramClazz == String.class) {
                        finalParameterValue[i] = value;
                    } else if (paramClazz == Integer.class || paramClazz == int.class) {
                        finalParameterValue[i] = Integer.valueOf(value);
                    } else if (paramClazz == Float.class || paramClazz == float.class) {
                        finalParameterValue[i] = Float.valueOf(value);
                    } else if (paramClazz == Double.class || paramClazz == double.class) {
                        finalParameterValue[i] = Double.valueOf(value);
                    }
                }
            } else {
                //如果没有注解就是其他类型：request，response，Map，domain对象等等
                Class paramClazz = parameter.getType();
                if (paramClazz.isArray()) {
                    //如果是数组类型暂时不做处理,抛出异常
                    throw new ParameterTypeException("对不起，暂时不能处理数组类型");
                } else {
                    if (paramClazz == HttpServletRequest.class) {
                        finalParameterValue[i] = request;continue;
                    }
                    if (paramClazz == HttpServletResponse.class) {
                        finalParameterValue[i] = response;continue;
                    }
                    if (paramClazz == Map.class) {
                        //如果是接口类型也不做处理
                        throw new ParameterTypeException("接口暂时不做处理");
                    }
                }
                //接下来是普通对象
                Object paramObject = null;
                try {
                    paramObject = paramClazz.getDeclaredConstructor().newInstance();
                } catch (java.lang.NoSuchMethodException e) {
                    e.printStackTrace();
                }
                if (paramObject instanceof Map) {
                    //如果试集合对象，创建一个集合
                    Map<String,Object> paramMap =(Map<String,Object> )paramObject;
                    Enumeration en = request.getParameterNames();
                    while (en.hasMoreElements()) {
                        String key = (String) en.nextElement();
                        String value = request.getParameter(key);
                        paramMap.put(key, value);
                    }
                    finalParameterValue[i] = paramMap;
                } else if (paramObject instanceof Object) {
                    //最后一个是domain对象
                    //获取对象中的全部属性
                    Field[] fields = paramClazz.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        String key = field.getName();
                        String value = request.getParameter(key);
                        //获取对象属性的数据类型
                        Class fieldType = field.getType();
                        Constructor fieldConstructor = null;
                        try {
                            fieldConstructor = fieldType.getConstructor(String.class);
                        } catch (java.lang.NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                        field.set(paramObject, fieldConstructor.newInstance(value));
                    }
                    finalParameterValue[i] = paramObject;
                } else {
                    throw new ParameterTypeException("对不起，实在是处理不了了");
                }
            }
        }
        return finalParameterValue;
    }

    /**
     * 这个小弟用于解析控制类中方法执行后的返回值
     * 并进行转发或重定向，或直接out写回
     *只能解析返回值是字符串的方法，并且该String只能代表是资源名
     * 参数就是方法返回的String字符串,没有返回值
     * */
    private void parseResponseString(String viewName,Method method,HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        //先做个判断,判断method上是否有注解responseBody，如果有则返回值String不是资源名路径viewName
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        if(responseBody!=null){
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(viewName);
        }else {
            //先做个判断，是否有String返回，或是否为null；
            if (!"".equals(viewName) && !"null".equals(viewName)) {
                //然后将字符串按照：拆分如果资源路径前有 redirect：则是重定向
                String[] viewNames = viewName.split(":");
                //如果viewNames长度为1，证明没有做说明,是一个转发
                if (viewNames.length == 1) {
                    request.getRequestDispatcher(viewNames[0]).forward(request, response);
                } else {//否则认为是一个重定向
                    if ("redirect".equals(viewNames[0])) {
                        response.sendRedirect(viewNames[1]);
                    }
                }
            } else {//否则就没有返回值，或返回值为null，抛出异常
                throw new ViewNameFormatException("您的方法返回值不规范，不做处理");
            }
        }

    }

    /**
     * 如果controller中方法要在request作用域中存值
     * 创建一个新的小弟
     * */
    private void parseModelAndView(Object obj,ModelAndView mv,HttpServletRequest request){
        //先获取ModelView集合中的map
        HashMap<String,Object> attributeMap = mv.getAttributeMap();
        //获取map中所有的key
        Set<String> keySet = attributeMap.keySet();
        Iterator<String> itKeySet = keySet.iterator();
        //然后遍历
        while(itKeySet.hasNext()){
            String key = itKeySet.next();
            Object value = attributeMap.get(key);
            request.setAttribute(key,value);
        }
        //然后获取注解，看是够需要在session作用域中存值
        SessionAttributes sessionAttributes = obj.getClass().getAnnotation(SessionAttributes.class);
        if(sessionAttributes!=null){
            //需要向session作用域中存值
            //获取注解中的String[]数组
            String[] attributeNames = sessionAttributes.value();
            if(attributeNames.length!=0){
                HttpSession session = request.getSession();
                for(String name:attributeNames){
                    session.setAttribute(name,attributeMap.get(name));
                }
            }
        }

    }

    /**
     * 这个方法做最终的总处理，
     * 可以处理String,ModelAndView或者其他Object对象（list，User等等）
     * */
    void finalResolver(Object obj,Method method,Object methodReturnResult,HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        if(methodReturnResult==null){
            //证明没有返回值，不需要处理
            return;
        }
        if(methodReturnResult instanceof String){
            this.parseResponseString((String)methodReturnResult,method,request,response);
        }else if(methodReturnResult instanceof ModelAndView){
            ModelAndView mv = (ModelAndView)methodReturnResult;
            this.parseModelAndView(obj,mv,request);
            this.parseResponseString(mv.getViewName(),method,request,response);
        }else {
            //返回值可能是一些对象 集合等等
            ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
            if(responseBody!=null){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("jsonObject",methodReturnResult);
                response.getWriter().write(jsonObject.toJSONString());
            }else {
                //就真的解决不了了
            }
        }
    }



}

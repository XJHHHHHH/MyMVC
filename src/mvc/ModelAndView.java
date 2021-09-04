package mvc;

import java.util.HashMap;

/**
 * 这个类相似与domain中的对象
 * 但它的作用是用于存储控制层方法的返回值.存储的是方法要在request作用域中存的值和方法返回的String
 * map<String,Object>是存值数据
 * String 是响应视图信息
 */
public class ModelAndView {

    private String viewName;
    private HashMap<String,Object> attributeMap = new HashMap<>();
    //下面两个set方法是交给用户使用
    //用于存储数据
    public void setViewName(String viewName){
        this.viewName = viewName;
    }
    public void addAttributeObject(String key,Object value){
        this.attributeMap.put(key,value);
    }

    //下面的get方法是给框架使用，获取信息做处理
    String getViewName(){
        return this.viewName;
    }

    Object getAttributeObject(String key){
        return this.attributeMap.get(key);
    }

    HashMap<String,Object> getAttributeMap(){
        return this.attributeMap;
    }

}

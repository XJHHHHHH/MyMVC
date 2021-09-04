package service;

import domain.User;

public class AtmService {

    public String login(User user){
        if("xjh".equals(user.getName())&&666==user.getPass()){
            return "success";
        }
        return "defeat";
    }
}

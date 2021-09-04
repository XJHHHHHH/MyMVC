package domain;

public class User {
    private String name;
    private Integer pass;

    public User(){}
    public User(String name,Integer pass){
        this.name = name;
        this.pass = pass;
    }

    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return  this.name;
    }

    public void setPass(Integer pass){
        this.pass = pass;
    }
    public Integer getPass(){
        return this.pass;
    }
}

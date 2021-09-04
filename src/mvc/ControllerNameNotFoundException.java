package mvc;

public class ControllerNameNotFoundException extends RuntimeException {
    public ControllerNameNotFoundException(){}
    public ControllerNameNotFoundException(String message){
        super(message);
    }
}

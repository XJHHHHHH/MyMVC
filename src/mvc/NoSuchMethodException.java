package mvc;

public class NoSuchMethodException extends RuntimeException {
    public NoSuchMethodException(){}
    public NoSuchMethodException(String message){
        super(message);
    }
}

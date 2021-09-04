package mvc;

public class ParameterTypeException extends RuntimeException {
    public ParameterTypeException(){}
    public ParameterTypeException(String message){
        super(message);
    }
}

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.lang.reflect.Method;
public class Scratch {
    public static void main(String[] args) throws Exception {
        System.out.println("Methods of HandlerMethodValidationException:");
        for (Method m : HandlerMethodValidationException.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
        System.out.println("Methods of tools.jackson.databind.exc.InvalidFormatException:");
        for (Method m : tools.jackson.databind.exc.InvalidFormatException.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
        System.out.println("Methods of tools.jackson.databind.JsonMappingException.Reference:");
        for (Method m : tools.jackson.databind.JsonMappingException.Reference.class.getMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}

import java.io.*;
import javax.script.*;

/**
 * Describe class TestJS here.
 *
 *
 * Created: Thu May 10 12:26:27 2007
 *
 * @author <a href="mailto:joakime@GRAYLING"></a>
 * @version 1.0
 */
public class TestJS {

    public static void main(String[] args) {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        Bindings bindings = new SimpleBindings();
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            bindings.put("test", "test");
            while ((line = reader.readLine()) != null) {
                Object result = engine.eval(line, bindings);
                if (result != null)
                    System.out.println(" => " + result);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

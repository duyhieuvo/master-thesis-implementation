package util;

public class EnvironmentVariableParser {
    public static Object getEnvironmentVariables(String envName, Object defaultValue) {
        Object result = null;
        String env = System.getenv(envName);
        if(env == null){
            result = defaultValue;
        }else{
            if(defaultValue instanceof Boolean){
                result = Boolean.valueOf(env);
            }else if(defaultValue instanceof Integer){
                result = Integer.valueOf(env);
            }else if(defaultValue instanceof Double){
                result = Double.valueOf(env);
            }else if(defaultValue instanceof Float){
                result = Float.valueOf(env);
            }else if(defaultValue instanceof Long){
                result = Long.valueOf(env);
            }else{
                result = env;
            }
        }
        return result;
    }
}

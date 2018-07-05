package com.devonfw.cobigen.templates.oasp4j.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Provides type operations, mainly checks and casts for Java Primitives, to be used in the templates
 *
 */
public class JavaUtil {

    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(JavaUtil.class);

    /**
     * The constructor.
     */
    public JavaUtil() {
        // Empty for CobiGen to automatically instantiate it
    }

    /**
     * Returns the Object version of a Java primitive or the input if the input isn't a java primitive
     *
     * @param simpleType
     *            String
     * @return the corresponding object wrapper type simple name of the input if the input is the name of a
     *         primitive java type. The input itself if not. (e.g. "int" results in "Integer")
     * @throws ClassNotFoundException
     *             should not occur.
     */
    public String boxJavaPrimitives(String simpleType) throws ClassNotFoundException {

        if (equalsJavaPrimitive(simpleType)) {
            return ClassUtils.primitiveToWrapper(ClassUtils.getClass(simpleType)).getSimpleName();
        } else {
            return simpleType;
        }

    }

    /**
     * Returns the simple name of the type of a field in the pojoClass. If the type is a java primitive the
     * name of the wrapper class is returned
     *
     * @param pojoClass
     *            {@link Class} the class object of the pojo
     * @param fieldName
     *            {@link String} the name of the field
     * @return String. The simple name of the field's type. The simple name of the wrapper class in case of
     *         java primitives
     * @throws NoSuchFieldException
     *             indicating something awefully wrong in the used model
     * @throws SecurityException
     *             if the field cannot be accessed.
     */
    public String boxJavaPrimitives(Class<?> pojoClass, String fieldName)
        throws NoSuchFieldException, SecurityException {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }

        if (equalsJavaPrimitive(pojoClass, fieldName)) {
            return ClassUtils.primitiveToWrapper(pojoClass.getDeclaredField(fieldName).getType()).getSimpleName();
        } else {
            Field field = pojoClass.getDeclaredField(fieldName);
            if (field == null) {
                field = pojoClass.getField(fieldName);
            }
            if (field == null) {
                throw new IllegalAccessError("Could not find field " + fieldName + " in class " + pojoClass);
            } else {
                return field.getType().getSimpleName();
            }
        }
    }

    /**
     * Checks if the given type is a Java primitive
     *
     * @param simpleType
     *            the type to be checked
     * @return true iff simpleType is a Java primitive
     */
    public boolean equalsJavaPrimitive(String simpleType) {

        try {
            return ClassUtils.getClass(simpleType).isPrimitive();
        } catch (ClassNotFoundException e) {
            LOG.warn("{}: Could not find {}", e.getMessage(), simpleType);
            return false;
        }
    }

    /**
     * Checks if the type of the field in the pojo's class is a java primitive
     *
     * @param pojoClass
     *            the {@link Class} object of the pojo
     * @param fieldName
     *            the name of the field to be checked
     * @return true iff the field is a java primitive
     * @throws NoSuchFieldException
     *             indicating something awefully wrong in the used model
     * @throws SecurityException
     *             if the field cannot be accessed.
     */
    public boolean equalsJavaPrimitive(Class<?> pojoClass, String fieldName)
        throws NoSuchFieldException, SecurityException {

        if (pojoClass == null) {
            return false;
        }

        Field field = pojoClass.getDeclaredField(fieldName);
        if (field == null) {
            field = pojoClass.getField(fieldName);
        }
        if (field == null) {
            return false;
        } else {
            return field.getType().isPrimitive();
        }
    }

    /**
     * Checks if the given type is a Java primitive or a Java primitive array
     *
     * @param simpleType
     *            the Type name to be checked
     * @return true iff {@link #equalsJavaPrimitive(String)} is true or if simpleType is an array with a
     *         primitive component
     */
    public boolean equalsJavaPrimitiveIncludingArrays(String simpleType) {

        Class<?> klasse;

        try {
            klasse = ClassUtils.getClass(simpleType).getComponentType();
        } catch (ClassNotFoundException e) {
            LOG.warn("{}: Could not find {}", e.getMessage(), simpleType);
            return false;
        }
        return equalsJavaPrimitive(simpleType) || (klasse != null && klasse.isPrimitive());
    }

    /**
     * Checks if the given field in the pojo class is a java primitive or an array of java primitives
     *
     * @param pojoClass
     *            the class object of the pojo
     * @param fieldName
     *            the name of the field to be checked
     * @return true iff {@link #equalsJavaPrimitive(Class, String)} is true or the field is an array of
     *         primitives
     * @throws NoSuchFieldException
     *             indicating something awfully wrong in the used model
     * @throws SecurityException
     *             if the field cannot be accessed.
     */
    public boolean equalsJavaPrimitiveIncludingArrays(Class<?> pojoClass, String fieldName)
        throws NoSuchFieldException, SecurityException {

        return equalsJavaPrimitive(pojoClass, fieldName) || (pojoClass.getDeclaredField(fieldName).getType().isArray()
            && pojoClass.getDeclaredField(fieldName).getType().getComponentType().isPrimitive());
    }

    /**
     * Returns a cast statement for a given (java primitive, variable name) pair or nothing if the type isn't
     * a java primitive
     *
     * @param simpleType
     *            Java Type
     * @param varName
     *            Variable name
     * @return String either of the form '((Java Primitive Object Type)varName)' if simpleType is a primitive
     *         or the empty String otherwise
     * @throws ClassNotFoundException
     *             should not occur
     */
    public String castJavaPrimitives(String simpleType, String varName) throws ClassNotFoundException {

        if (equalsJavaPrimitive(simpleType)) {
            return String.format("((%1$s)%2$s)", boxJavaPrimitives(simpleType), varName);
        } else {
            return "";
        }
    }

    /**
     * Returns a cast statement for a given (java primitive, variable name) pair or nothing if the type isn't
     * a java primitive
     *
     * @param pojoClass
     *            the class object of the pojo
     * @param fieldName
     *            the name of the field to be casted
     * @return if fieldName points to a primitive field then a casted statement (e.g. for an int field:
     *         '((Integer)field)') or an empty String otherwise
     * @throws NoSuchFieldException
     *             indicating something awefully wrong in the used model
     * @throws SecurityException
     *             if the field cannot be accessed.
     */
    public String castJavaPrimitives(Class<?> pojoClass, String fieldName)
        throws NoSuchFieldException, SecurityException {

        if (equalsJavaPrimitive(pojoClass, fieldName)) {
            return String.format("((%1$s)%2$s)", boxJavaPrimitives(pojoClass, fieldName), fieldName);
        } else {
            return "";
        }
    }

    /**
     * @param pojoClass
     *            {@link Class} the class object of the pojo
     * @param fieldName
     *            {@link String} the name of the field
     * @return true if the field is an instance of java.utils.Collections
     * @throws NoSuchFieldException
     *             indicating something awefully wrong in the used model
     * @throws SecurityException
     *             if the field cannot be accessed.
     */
    public boolean isCollection(Class<?> pojoClass, String fieldName) throws NoSuchFieldException, SecurityException {

        if (pojoClass == null) {
            return false;
        }

        Field field = pojoClass.getDeclaredField(fieldName);
        if (field == null) {
            field = pojoClass.getField(fieldName);
        }
        if (field == null) {
            return false;
        } else {
            return Collection.class.isAssignableFrom(field.getType());
        }

    }

    /**
     * Returns the Ext Type to a given java type
     *
     * @param simpleType
     *            any java type's simple name
     * @return corresponding Ext type
     */
    public String getExtType(String simpleType) {

        switch (simpleType) {
        case "short":
        case "Short":
        case "int":
        case "Integer":
        case "long":
        case "Long":
            return "Integer";
        case "float":
        case "Float":
        case "double":
        case "Double":
            return "Number";
        case "boolean":
        case "Boolean":
            return "Boolean";
        case "char":
        case "Character":
        case "String":
            return "String";
        case "Date":
            return "Date";
        default:
            return "Field";
        }
    }

    /**
     * Returns the angular Type to a given java type
     *
     * @param simpleType
     *            any java type's simple name
     * @return corresponding angular type
     */
    public String getSimpleType(String simpleType) {
        switch (simpleType) {
        case "EAJava_int":
        case "byte":
        case "short":
        case "int":
        case "Integer":
        case "long":
        case "Long":
        case "float":
        case "Double":
        case "double":
            return "number";
        case "boolean":
        case "Boolean":
            return "Boolean";
        case "char":
        case "String":
            return "String";
        default:
            return "any";
        }
    }

    /**
     * returns the Angular5 type associated with a Java primitive
     *
     * @param simpleType
     *            :{@link String} the type to be parsed
     * @return the corresponding Angular type or 'any' otherwise
     */
    public String getAngularType(String simpleType) {

        switch (simpleType) {
        case "boolean":
            return "boolean";
        case "Boolean":
            return "boolean";
        case "short":
            return "number";
        case "Short":
            return "number";
        case "int":
            return "number";
        case "Integer":
            return "number";
        case "long":
            return "number";
        case "Long":
            return "number";
        case "float":
            return "number";
        case "Float":
            return "number";
        case "double":
            return "number";
        case "Double":
            return "number";
        case "char":
            return "string";
        case "Character":
            return "string";
        case "String":
            return "string";
        case "byte":
            return "number";
        default:
            return "any";
        }
    }

    /**
     * This method is to check if a method has a certain annotation
     *
     * @param pojoClass
     *            {@link Class} the class object of the pojo
     * @param name
     *            {@link String} the name of the method
     * @param annotation
     *            {@link String} the name of the annotation
     * @return true if the field has the annotation, false if not
     */
    public boolean hasAnnotation(Class<?> pojoClass, String name, String annotation) {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }
        for (Method method : pojoClass.getMethods()) {
            if (method.getName().contains(name)) {
                for (Annotation anno : method.getAnnotations()) {
                    if (anno.annotationType().getCanonicalName().equals(annotation)) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    /**
     * Checks if the class has at least one method with the requested annotation
     *
     * @param pojoClass
     *            {@link Class} the class object of the pojo
     * @param annotation
     *            {@link String} the name of the annotation
     * @return true if at least one method of the class' methods has the requested annotation
     */
    public boolean hasMethodWithAnnotation(Class<?> pojoClass, String annotation) {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }
        for (Method method : pojoClass.getMethods()) {
            for (Annotation anno : method.getAnnotations()) {
                if (anno.annotationType().getCanonicalName().equals(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * returns the class name of the return type of a specific method.
     * @param pojoClass
     *            {@link Class}&lt;?> the class object of the pojo
     * @param name
     *            {@link String} the name of the method
     * @return the class name of the return type of the specified method
     */
    public String getReturnType(Class<?> pojoClass, String name) {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }
        String s = "-";
        Method method = findMethod(pojoClass, name);
        if (method != null && !method.getReturnType().equals(Void.TYPE)) {
            s = method.getReturnType().toString();
            s = s.substring(s.lastIndexOf('.') + 1, s.length());
        }
        return s;
    }

    public String getParams(Class<?> pojoClass, String methodName, Map<String, Object> javaDoc) throws Exception {

        String result = "";
        Method m = findMethod(pojoClass, methodName);
        if (m.getParameterCount() < 1) {
            return "!-!-!-!-";
        }
        if (m.getParameterCount() == 1) {
            if (!m.getParameters()[0].getType().isPrimitive()) {
                return "!-!-!-!-";
            }
        }
        for (Parameter param : m.getParameters()) {
            // Add the name of the parameter as path or query parameter
            boolean isPath = param.isAnnotationPresent(javax.ws.rs.PathParam.class);
            boolean isQuery = param.isAnnotationPresent(javax.ws.rs.QueryParam.class);
            if (isPath || isQuery) {
                result += "!";
                if (isPath) {
                    result +=
                        "{" + param.getAnnotation(javax.ws.rs.PathParam.class).value() + "}" + System.lineSeparator();
                } else if (isQuery) {
                    result += "?" + param.getAnnotation(javax.ws.rs.QueryParam.class).value() + System.lineSeparator();
                }

                // Add the type
                String type = param.getType().getSimpleName();
                result += "!" + type + System.lineSeparator();

                // Add the constraints
                result += "!";
                int counter = 0;
                for (Annotation anno : param.getAnnotations()) {
                    String annoName = anno.annotationType().getName();
                    Pattern p = Pattern.compile("javax\\.validation\\.constraints\\.([^\\.]*)");
                    Matcher match = p.matcher(annoName);
                    if (match.find()) {
                        counter++;
                        String shortName = annoName.substring(annoName.lastIndexOf('.') + 1);
                        Object value;
                        Method method;
                        try {
                            method = anno.getClass().getMethod("value");
                            value = method.invoke(anno);
                            result += shortName + " = " + value + " +" + System.lineSeparator();
                        } catch (NoSuchMethodException e) {
                            if (shortName.equals("NotNull")) {
                                result += "[red]#__Required__# +" + System.lineSeparator();
                            } else {
                                result += shortName + " +" + System.lineSeparator();
                            }
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | SecurityException e) {
                            throw new Exception(e.getMessage());
                        }
                    }
                }
                if (counter == 0) {
                    result += "-" + System.lineSeparator();
                }

                // Add Javadoc
                Map<String, String> params = (Map<String, String>) javaDoc.get("params");
                result += "!" + getJavaDocWithoutLink(params.get(param.getName())) + " +" + System.lineSeparator();
            }
        }
        return result;
    }

    public String getJavaDocWithoutLink(String doc) {

        Pattern p = Pattern.compile("(\\{@link ([^\\}]*)\\})");
        Matcher m = p.matcher(doc);
        while (m.find()) {
            doc = doc.replace(m.group(1), m.group(2));
        }
        return doc;
    }

    public String extractMediaType(String input) {

        if (input.contains("MediaType.APPLICATION_JSON")) {
            input = input.replace("MediaType.APPLICATION_JSON", MediaType.APPLICATION_JSON);
        }
        if (input.contains("MediaType.APPLICATION_XML")) {
            input = input.replace("MediaType.APPLICATION_XML", MediaType.APPLICATION_XML);
        }
        return input;
    }

    public String extractRootPath(Class<?> pojoClass) throws IOException {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }
        String t = "";
        StringBuilder sb = new StringBuilder("http://localhost:");
        InputStream in = pojoClass.getClassLoader().getResourceAsStream("application.properties");
        if (in != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((t = br.readLine()) != null) {
                if (t.matches("#server\\.port=(\\d{0,5})") || t.matches("#server\\.context-path=([^\\s]*)")) {
                    sb.append(t.substring(t.indexOf('=') + 1));
                }
            }
            return sb.toString();
        } else {
            throw new IOException("application.properties file not found!");
        }
    }

    private Method findMethod(Class<?> pojoClass, String methodName) {

        if (pojoClass == null) {
            throw new IllegalAccessError(
                "Class object is null. Cannot generate template as it might obviously depend on reflection.");
        }
        for (Method m : pojoClass.getMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Create a response in JSON format, iterating through non-primitive types to get their data as well
     * @param pojoClass
     *            The input class
     * @param methodName
     *            The name of the operation to get the response of
     * @return A JSON representation of the response object
     * @throws Exception
     */
    public String getJSONResponseBody(Class<?> pojoClass, String methodName) throws Exception {
        Class<?> responseType = findMethod(pojoClass, methodName).getReturnType();
        if (hasBody(pojoClass, methodName, true)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                Object obj = responseType.newInstance();
                return "...." + System.lineSeparator() + mapper.writeValueAsString(obj) + System.lineSeparator()
                    + "....";
            } catch (InstantiationException | IllegalAccessException | JsonProcessingException e) {
                throw new Exception(e.getMessage());
            }
        }
        return "-";
    }

    /**
     * Create a request in JSON format, iterating through non-primitive types to get their data as well
     * @param pojoClass
     *            The input class
     * @param methodName
     *            The name of the operation to get the request of
     * @return A JSON representation of the request object
     * @throws Exception
     */
    public String getJSONRequestBody(Class<?> pojoClass, String methodName) throws Exception {
        Method m = findMethod(pojoClass, methodName);
        if (hasBody(pojoClass, methodName, false)) {
            Parameter param = m.getParameters()[0];
            Class<?> requestType = param.getType();
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                Object obj = requestType.newInstance();
                return "...." + System.lineSeparator() + mapper.writeValueAsString(obj) + System.lineSeparator()
                    + "....";
            } catch (InstantiationException | IllegalAccessException | JsonProcessingException e) {
                throw new Exception(e.getMessage());
            }
        }
        return "-";
    }

    public boolean hasBody(Class<?> pojoClass, String methodName, boolean isResponse) {
        Method m = findMethod(pojoClass, methodName);
        if (isResponse) {
            Class<?> returnType = m.getReturnType();
            if (!returnType.isPrimitive() && !returnType.equals(Void.TYPE)) {
                return true;
            }
        } else {
            if (m.getParameterCount() == 1) {
                Parameter param = m.getParameters()[0];
                Class<?> requestType = param.getType();
                if (!requestType.isPrimitive() && !requestType.equals(Void.TYPE)
                    && !param.isAnnotationPresent(javax.ws.rs.PathParam.class)
                    && !param.isAnnotationPresent(javax.ws.rs.QueryParam.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getRequestType(Map<String, Object> annotations) throws Exception {
        if (annotations.containsKey("javax_ws_rs_POST")) {
            return "POST";
        } else if (annotations.containsKey("javax_ws_rs_PUT")) {
            return "PUT";
        } else if (annotations.containsKey("javax_ws_rs_GET")) {
            return "GET";
        } else if (annotations.containsKey("javax_ws_rs_DELETE")) {
            return "DELETE";
        } else if (annotations.containsKey("javax_ws_rs_PATCH")) {
            return "PATCH";
        } else {
            return "-";
        }
    }

}

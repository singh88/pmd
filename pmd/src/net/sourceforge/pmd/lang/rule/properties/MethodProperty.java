/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.lang.rule.properties;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;

import net.sourceforge.pmd.util.ClassUtil;
import net.sourceforge.pmd.util.StringUtil;

/**
 * Defines a property type that can specify one or more methods to use as part of a rule.
 *
 * Rule developers can limit the rules to those within designated packages per the
 * 'legalPackages' argument in the constructor which can be an array of partial
 * package names, i.e., ["java.lang", "com.mycompany" ].
 *
 * @author Brian Remedios
 */
public class MethodProperty extends AbstractPackagedProperty {

    public static final char CLASS_METHOD_DELIMITER = '#';
    public static final char METHOD_ARG_DELIMITER = ',';
    public static final char[] METHOD_GROUP_DELIMITERS = new char[] { '(', ')' };

    private static final String ARRAY_FLAG = "[]";
    private static final Map<Class, String> TYPE_SHORTCUTS = ClassUtil.getClassShortNames();

    /**
     * Method shortestNameFor.
     * @param cls Class<?>
     * @return String
     */
    private static String shortestNameFor(Class<?> cls) {
        String compactName = TYPE_SHORTCUTS.get(cls);
        return compactName == null ? cls.getName() : compactName;
    }

    /**
     * Return the value of `method' as a string that can be easily recognized and parsed
         * when we see it again.
         *
     * @param method the method to convert
     * @return the string value
     */
    public static String asStringFor(Method method) {
        StringBuilder sb = new StringBuilder();
        asStringOn(method, sb);
        return sb.toString();
    }

    /**
     * Return the value as a string that can be easily recognized and parsed
     * when we see it again.
     *
     * @param value Object
     * @return String
     */
    @Override
    protected String asString(Object value) {
        return value == null ? "" : asStringFor((Method) value);
    }

    /**
     * Method serializedTypeIdOn.
     * @param type Class<?>
     * @param sb StringBuilder
     */
    private static void serializedTypeIdOn(Class<?> type, StringBuilder sb) {

        Class<?> arrayType = type.getComponentType();
        if (arrayType == null) {
            sb.append(shortestNameFor(type));
            return;
        }
        sb.append(shortestNameFor(arrayType)).append(ARRAY_FLAG);
    }

    /**
     * Serializes the method signature onto the specified buffer.
     *
     * @param method Method
     * @param sb StringBuilder
     */
    public static void asStringOn(Method method, StringBuilder sb) {

        Class<?> clazz = method.getDeclaringClass();

        sb.append(shortestNameFor(clazz));
        sb.append(CLASS_METHOD_DELIMITER);
        sb.append(method.getName());

        sb.append(METHOD_GROUP_DELIMITERS[0]);

        Class<?>[] argTypes = method.getParameterTypes();
        if (argTypes.length == 0) {
            sb.append(METHOD_GROUP_DELIMITERS[1]);
            return;
        }

        serializedTypeIdOn(argTypes[0], sb);
        for (int i = 1; i < argTypes.length; i++) {
            sb.append(METHOD_ARG_DELIMITER);
            serializedTypeIdOn(argTypes[i], sb);
        }
        sb.append(METHOD_GROUP_DELIMITERS[1]);
    }

    /**
     * Method typeFor.
     * @param typeName String
     * @return Class<?>
     */
    private static Class<?> typeFor(String typeName) {

        Class<?> type = null;

        if (typeName.endsWith(ARRAY_FLAG)) {
            String arrayTypeName = typeName.substring(0, typeName.length() - ARRAY_FLAG.length());
            type = typeFor(arrayTypeName); // recurse
            return Array.newInstance(type, 0).getClass(); // TODO is there a better way to get an array type?
        }

        type = ClassUtil.getTypeFor(typeName); // try shortcut first
        if (type != null) {
            return type;
        }

        try {
            return Class.forName(typeName);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns the method specified within the string argument after parsing out its source class and
     * any optional arguments. Callers need to specify the delimiters expected between the various
     * elements.  I.e.:
     *
     * 	"String#isEmpty()"
     *  "String#indexOf(int)"
     *  "String#substring(int,int)"
     *
     *  If a method isn't part of the specified class we will walk up any superclasses to Object to try
     *  and find it.
     *
     *  If the classes are listed in the ClassUtil class within in Typemaps then you likely can avoid
     *  specifying fully-qualified class names per the above example.
     *
     *  Returns null if a matching method cannot be found.
     *
     * @param methodNameAndArgTypes
     * @param classMethodDelimiter
     * @param methodArgDelimiter
     * @return Method
     */
    public static Method methodFrom(String methodNameAndArgTypes, char classMethodDelimiter, char methodArgDelimiter) {

        // classname#methodname(arg1,arg2)
        //          0          1         2

        int delimPos0 = methodNameAndArgTypes.indexOf(classMethodDelimiter);
        if (delimPos0 < 0) {
            return null;
        }

        String className = methodNameAndArgTypes.substring(0, delimPos0);
        Class<?> type = ClassUtil.getTypeFor(className);
        if (type == null) {
            return null;
        }

        int delimPos1 = methodNameAndArgTypes.indexOf(METHOD_GROUP_DELIMITERS[0]);
        if (delimPos1 < 0) {
            String methodName = methodNameAndArgTypes.substring(delimPos0 + 1);
            return ClassUtil.methodFor(type, methodName, ClassUtil.EMPTY_CLASS_ARRAY);
        }

        String methodName = methodNameAndArgTypes.substring(delimPos0 + 1, delimPos1);
        if (StringUtil.isEmpty(methodName)) {
            return null;
        } // missing method name?

        int delimPos2 = methodNameAndArgTypes.indexOf(METHOD_GROUP_DELIMITERS[1]);
        if (delimPos2 < 0) {
            return null;
        } // error!

        String argTypesStr = methodNameAndArgTypes.substring(delimPos1 + 1, delimPos2);
        if (StringUtil.isEmpty(argTypesStr)) {
            return ClassUtil.methodFor(type, methodName, ClassUtil.EMPTY_CLASS_ARRAY);
        } // no arg(s)

        String[] argTypeNames = StringUtil.substringsOf(argTypesStr, methodArgDelimiter);
        Class<?>[] argTypes = new Class[argTypeNames.length];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = typeFor(argTypeNames[i]);
        }

        return ClassUtil.methodFor(type, methodName, argTypes);
    }

    /**
     * Constructor for MethodProperty.
     *
     * @param theName        String
     * @param theDescription String
     * @param theDefault     Method
     * @param legalPackageNames String[]
     * @param theUIOrder     float
     */
    public MethodProperty(String theName, String theDescription, Method theDefault, String[] legalPackageNames,
            float theUIOrder) {
        super(theName, theDescription, theDefault, legalPackageNames, theUIOrder);

        isMultiValue(false);
        multiValueDelimiter(' ');
    }

    /**
     * Constructor for MethodProperty.
     *
     * @param theName        String
     * @param theDescription String
     * @param theDefaults    Method[]
     * @param legalPackageNames String[]
     * @param theUIOrder     float
     */
    public MethodProperty(String theName, String theDescription, Method[] theDefaults, String[] legalPackageNames,
            float theUIOrder) {
        super(theName, theDescription, theDefaults, legalPackageNames, theUIOrder);

        isMultiValue(true);
        multiValueDelimiter(' ');
    }

    /**
     * Method packageNameOf.
     * @param item Object
     * @return String
     */
    @Override
    protected String packageNameOf(Object item) {

        final Method method = (Method) item;
        return method.getDeclaringClass().getName() + '.' + method.getName();
    }

    /**
     * Method itemTypeName.
     * @return String
     */
    @Override
    protected String itemTypeName() {
        return "method";
    }

    /**
     * Method type.
     *
     * @return Class
     * @see net.sourceforge.pmd.PropertyDescriptor#type()
     */
    public Class<Method> type() {
        return Method.class;
    }

    /**
     * Method valueFrom.
     *
     * @param valueString  String
     * @return Object
     * @throws IllegalArgumentException
     * @see net.sourceforge.pmd.PropertyDescriptor#valueFrom(String)
     */
    public Object valueFrom(String valueString) throws IllegalArgumentException {

        if (!isMultiValue()) {
            return methodFrom(valueString, CLASS_METHOD_DELIMITER, METHOD_ARG_DELIMITER);
        }

        String[] values = StringUtil.substringsOf(valueString, multiValueDelimiter());

        Method[] methods = new Method[values.length];
        for (int i = 0; i < methods.length; i++) {
            methods[i] = methodFrom(values[i], CLASS_METHOD_DELIMITER, METHOD_ARG_DELIMITER);
        }
        return methods;
    }
}

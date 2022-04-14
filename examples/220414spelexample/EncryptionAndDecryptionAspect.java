package com.jianf.commons.aop.aspect;

import com.jianf.commons.aop.annotation.*;
import com.jianf.commons.utils.AesUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Integer.MAX_VALUE;

/**
 * 动态代理 代理spring解析器为request params类型
 *
 * @author fengjian
 */
@Aspect
@Component
public class EncryptionAndDecryptionAspect {
    private static final String REGEX = ",";
    private static final String STRING = ".";
    public static final char FIELD_JOIN = '_';
    private final Logger logger = LoggerFactory.getLogger(EncryptionAndDecryptionAspect.class);

    private static final String SET = "set";

    @Pointcut("execution(* com.jianf..mapper.*Mapper.*(..))")
    public void invoke() {
    }

    /**
     * 参数加密 注意：参数暂时只支持单一参数 并且参数为string类型和实体bean类型
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Order(MAX_VALUE)
    @Around("invoke()")
    public Object parameterEncryptProxy(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Object[] getParameter = jp.getArgs();
        ParameterEncrypt encrypt = method.getAnnotation(ParameterEncrypt.class);
        if (encrypt != null) {
            String[] getSpelExpression = encrypt.spelExpression().split(REGEX);
            Object o = getParameter[0];
            // string 类型的 因业务场景需要并未处理其他类型 后续有需求再迭代开发
            if (o instanceof String) {
                String encryptFiled = AesUtils.encrypt(o.toString());
                getParameter[0] = encryptFiled;
            } else if (o instanceof List) {
                List<Object> list = (List) o;
                for (Object object : list) {
                    for (int i = 0; i < getSpelExpression.length; i++) {
                        invokeEncrypt(getSpelExpression[i], object);
                    }

                }
            } else if (o instanceof Object) {
                for (int i = 0; i < getSpelExpression.length; i++) {
                    invokeEncrypt(getSpelExpression[i], o);
                }
            }
        }
        return jp.proceed(getParameter);
    }

    /**
     * 参数解密 注意：参数暂时只支持单一参数 并且参数为string类型和实体bean类型
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Order(MAX_VALUE)
    @Around("invoke()")
    public Object parameterDecryptProxy(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Object[] getParameter = jp.getArgs();
        ParameterDecrypt decrypt = method.getAnnotation(ParameterDecrypt.class);
        if (decrypt != null) {
            decryptMethod(getParameter, decrypt.spelExpression());
        }
        return jp.proceed(getParameter);
    }

    /**
     * 解密方法
     *
     * @param getParameter
     * @param s
     * @throws NoSuchMethodException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void decryptMethod(Object[] getParameter, String s) throws NoSuchMethodException, IntrospectionException, InvocationTargetException, IllegalAccessException {
        String[] getSpelExpression = s.split(REGEX);
        Object o = getParameter[0];
        // string 类型的 因业务场景需要并未处理其他类型
        if (o instanceof String) {
            String encryptFiled = AesUtils.decrypt(o.toString());
            getParameter[0] = encryptFiled;
        } else if (o instanceof Object) {
            for (int i = 0; i < getSpelExpression.length; i++) {
                invokeDecrypt(getSpelExpression[i], o);
            }
        }
    }


    @Order(MAX_VALUE)
    @Around(value = "@annotation(com.jianf.commons.aop.annotation.DecryptRoute)")
    public Object parameterDecryptProxyForController(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Object[] getParameter = jp.getArgs();
        DecryptRoute decrypt = method.getAnnotation(DecryptRoute.class);
        if (decrypt != null) {
            decryptMethod(getParameter, decrypt.spelExpression());
        }
        return jp.proceed(getParameter);
    }


    /**
     * 返回值解密  注意：参数暂时只支持单一参数 并且参数为string类型和实体bean类型
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Order(MAX_VALUE)
    @Around("invoke()")
    public Object returnDecryptProxy(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Object object = jp.proceed();
        ReturnDecrypt encrypt = method.getAnnotation(ReturnDecrypt.class);
        if (encrypt != null) {
            String[] getSpelExpression = encrypt.spelExpression().split(REGEX);

            if (object instanceof List) {
                List<Object> o = (List<Object>) object;
                for (Object o1 : o) {
                    for (int i = 0; i < getSpelExpression.length; i++) {
                        //map类型特殊处理
                        if (o1 instanceof Map) {
                            ((Map) o1).put(getSpelExpression[i], AesUtils.decrypt(((Map) o1).get(getSpelExpression[i]).toString()));
                        } else {
                            invokeDecrypt(getSpelExpression[i], o1);
                        }
                    }
                }
            } else if (object instanceof Map) {
                for (int i = 0; i < getSpelExpression.length; i++) {
                    Object decryptParameters = ((Map) object).get(getSpelExpression[i]);
                    if (decryptParameters != null) {
                        ((Map) object).put(getSpelExpression[i], AesUtils.decrypt(decryptParameters.toString()));
                    }
                }

            } else if (object instanceof Object) {
                for (int i = 0; i < getSpelExpression.length; i++) {
                    invokeDecrypt(getSpelExpression[i], object);
                }
            }
        }
        return object;
    }

    /**
     * 返回值加密 注意：参数暂时只支持单一参数 并且参数为string类型和实体bean类型
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Order(MAX_VALUE)
    @Around("invoke()")
    public Object returnEncryptProxy(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Object object = jp.proceed();
        ReturnEncrypt encrypt = method.getAnnotation(ReturnEncrypt.class);
        if (encrypt != null) {
            String[] getSpelExpression = encrypt.spelExpression().split(REGEX);

            if (object instanceof List) {
                List<Object> o = (List<Object>) object;
                for (Object o1 : o) {
                    for (int i = 0; i < getSpelExpression.length; i++) {
                        invokeEncrypt(getSpelExpression[i], o1);
                    }
                }
            } else if (object instanceof Map) {
                for (int i = 0; i < getSpelExpression.length; i++) {
                    Object decryptParameters = ((Map) object).get(getSpelExpression[i]);
                    ((Map) object).put(getSpelExpression[i], AesUtils.encrypt(decryptParameters.toString()));
                }
            } else if (object instanceof Object) {
                for (int i = 0; i < getSpelExpression.length; i++) {
                    invokeEncrypt(getSpelExpression[i], object);
                }
            }
        }
        return object;
    }


    /**
     * 执行解密
     *
     * @param s
     * @param source
     * @throws NoSuchMethodException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void invokeDecrypt(String s, Object source) throws NoSuchMethodException, IntrospectionException, InvocationTargetException, IllegalAccessException {
        String spelExpression = s;
        String sourceFiled = null;
        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();
        int classNameBeginIndex = spelExpression.indexOf(STRING);
        context.setVariable(spelExpression.substring(1, classNameBeginIndex), source);
        Expression expression = parser.parseExpression(spelExpression);
        try {
            sourceFiled = (String) expression.getValue(context);
        } catch (SpelEvaluationException e) {
            logger.info("解析表达式字段属性为null");
            return;
        }
        if (StringUtils.isBlank(sourceFiled)) {
            return;
        }
        String encryptFiled = AesUtils.decrypt(sourceFiled);
        Object target = expression(spelExpression, source);
        Method targetMethod = target.getClass().getDeclaredMethod(parSetName(spelExpression.substring(spelExpression.lastIndexOf(STRING) + 1, spelExpression.length())), new Class[]{String.class});
        targetMethod.setAccessible(true);
        targetMethod.invoke(target, new Object[]{encryptFiled});
    }

    /**
     * 执行加密
     *
     * @param s
     * @param source
     * @throws NoSuchMethodException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void invokeEncrypt(String s, Object source) throws NoSuchMethodException, IntrospectionException, InvocationTargetException, IllegalAccessException {
        String spelExpression = s;
        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();
        int classNameBeginIndex = spelExpression.indexOf(STRING);
        context.setVariable(spelExpression.substring(1, classNameBeginIndex), source);
        String sourceFiled;
        Expression expression = parser.parseExpression(spelExpression);
        try {
            sourceFiled = (String) expression.getValue(context);
        } catch (SpelEvaluationException e) {
            logger.info("解析表达式字段属性为null");
            return;
        }
        String encryptFiled = AesUtils.encrypt(sourceFiled);
        Object target = expression(spelExpression, source);
        Method targetMethod = target.getClass().getDeclaredMethod(parSetName(spelExpression.substring(spelExpression.lastIndexOf(STRING) + 1, spelExpression.length())), new Class[]{String.class});
        targetMethod.setAccessible(true);
        targetMethod.invoke(target, new Object[]{encryptFiled});
    }


    /**
     * 构建set方法
     *
     * @param fieldName
     * @return
     */
    public static String parSetName(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return null;
        }
        int startIndex = 0;
        if (fieldName.charAt(0) == FIELD_JOIN) {
            startIndex = 1;
        }
        return SET + fieldName.substring(startIndex, startIndex + 1).toUpperCase() + fieldName.substring(startIndex + 1);
    }

    /**
     * 递归解析spel表达式
     *
     * @param spelExpression
     * @param source
     * @return
     * @throws NoSuchMethodException
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Object expression(String spelExpression, Object source) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        String[] spelExpressionSplit = spelExpression.split("\\.");
        int count = spelExpressionSplit.length;
        if (count == 1 || count == 0) {
            return source;
        } else {
            int isContainSign = spelExpression.indexOf('#');
            int isContainDot = spelExpression.indexOf(STRING);
            int beginIndex = isContainDot + 1;
            int endIndex = spelExpression.length();
            if (isContainSign >= 0) {
                return expression(spelExpression.substring(beginIndex, endIndex), source);
            }
            Field[] fields = source.getClass().getDeclaredFields();
            Object target = null;
            for (Field field : fields) {
                if (Objects.equals(field.getName(), spelExpressionSplit[0])) {
                    PropertyDescriptor pd = new PropertyDescriptor(field.getName(), source.getClass());
                    Method rM = pd.getReadMethod();
                    target = rM.invoke(source);
                    break;
                }
            }
            return expression(spelExpression.substring(beginIndex, endIndex), target);
        }
    }

}
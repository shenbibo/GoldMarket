package com.sky.goldmarket.retrofit.common;

import com.sky.goldmarket.retrofit.common.annotation.FormField;
import com.sky.goldmarket.retrofit.common.annotation.HeaderField;
import com.sky.goldmarket.retrofit.common.annotation.QueryField;
import com.sky.slog.Slog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 字段获取工具类
 * Created by sky on 2017/7/19.
 */
final class FieldUtils {

    /**
     * 从指定的对象中遍历以指定注解标识的字段，以名称-值得方式返回
     */
    static Map<String, String> parseFields(RequestBean requestBean, Class<? extends Annotation> annotationClazz) {
        Map<String, String> map = new HashMap<>();
        List<Field> fields = getField(requestBean.getClass(), annotationClazz);
        Object value;
        AnnotationParser parser = createAnnotationParse(annotationClazz);
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                value = field.get(requestBean);
                if (value == null) {
                    continue;
                }
                map.put(parser.apply(field), String.valueOf(value));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Slog.t(HttpUtils.TAG).i(map);
        return map;
    }

    private static List<Field> getField(Class<?> clazz, Class<? extends Annotation> annotationClazz) {
        List<Field> fields = new ArrayList<>();
        fields.addAll(getFieldWithAnnotation(clazz, annotationClazz));
        Class<?>[] classes = clazz.getInterfaces();

        // 递归获取其超类申明的字段
        if (classes.length != 0) {
            for (Class<?> tempClazz : classes) {
                fields.addAll(getField(tempClazz, annotationClazz));
            }
        }

        return fields;
    }

    private static List<Field> getFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> annotationFields = new ArrayList<>();

        for (Field field : fields) {
            if (field.getAnnotation(annotationClazz) != null) {
                annotationFields.add(field);
            }
        }

        return annotationFields;
    }

    private static AnnotationParser createAnnotationParse(Class<? extends Annotation> annotationClazz){
        if(annotationClazz == HeaderField.class){
            return HeaderParser.HEADER_PARSER;
        }else if(annotationClazz == QueryField.class){
            return QueryParser.QUERY_PARSER;
        }else if(annotationClazz == FormField.class){
            return FormParser.FORM_PARSER;
        }

        throw new RuntimeException("not support Annotation, class = " + annotationClazz.getName());
    }

    interface AnnotationParser {
        String apply(Field field);
    }

    private static class HeaderParser implements AnnotationParser {
        static final HeaderParser HEADER_PARSER = new HeaderParser();
        @Override
        public String apply(Field field) {
            HeaderField headerField = field.getAnnotation(HeaderField.class);
            return headerField.value().isEmpty() ? field.getName() : headerField.value();
        }
    }

    private static class QueryParser implements AnnotationParser {
        static final QueryParser QUERY_PARSER = new QueryParser();

        @Override
        public String apply(Field field) {
            QueryField queryField = field.getAnnotation(QueryField.class);
            return queryField.value().isEmpty() ? field.getName() : queryField.value();
        }
    }

    private static class FormParser implements AnnotationParser {
        static final FormParser FORM_PARSER = new FormParser();

        @Override
        public String apply(Field field) {
            FormField formField = field.getAnnotation(FormField.class);
            return formField.value().isEmpty() ? field.getName() : formField.value();
        }
    }
}

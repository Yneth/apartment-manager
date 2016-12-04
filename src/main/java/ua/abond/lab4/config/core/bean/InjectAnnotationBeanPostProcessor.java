package ua.abond.lab4.config.core.bean;

import org.apache.log4j.Logger;
import ua.abond.lab4.config.core.BeanPostProcessor;
import ua.abond.lab4.config.core.ConfigurableBeanFactory;
import ua.abond.lab4.config.core.Ordered;
import ua.abond.lab4.config.core.annotation.Inject;
import ua.abond.lab4.config.core.exception.BeanInstantiationException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class InjectAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered {
    private static final Logger logger = Logger.getLogger(InjectAnnotationBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(ConfigurableBeanFactory factory, Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(ConfigurableBeanFactory factory, Object bean, String simpleName) {
        Arrays.stream(bean.getClass().getDeclaredFields()).
                filter(f -> f.isAnnotationPresent(Inject.class)).
                forEach(f -> inject(factory, bean, f));
        Arrays.stream(bean.getClass().getDeclaredMethods()).
                filter(m -> m.isAnnotationPresent(Inject.class)).
                forEach(m -> inject(factory, bean, m));
        return bean;
    }

    private void inject(ConfigurableBeanFactory factory, Object bean, Field f) {
        logger.debug("Trying to inject " + f.getName() + " of type " + f.getType()
                + " to '" + bean.getClass().getSimpleName() + "'");

        Method setter = findSetter(bean, f);
        if (setter != null) {
            inject(factory, bean, setter);
        } else {
            Object inject = getBean(factory, f.getType());
            inject(bean, inject, f);
        }
    }

    private void inject(ConfigurableBeanFactory factory, Object bean, Method setter) {
        logger.debug(String.format("Trying to inject to method '%s' bean of type '%s' of '%s' object",
                setter.getName(), setter.getReturnType(), bean.getClass().getSimpleName()
        ));
        if (setter.getParameterCount() == 1 && isSetter(setter)) {
            Object inject = getBean(factory, setter.getParameterTypes()[0]);

            inject(bean, inject, setter);
        } else {
            throw new BeanInstantiationException(String.format("Setter '%s' of bean '%s' has invalid declaration.",
                    setter.getName(), bean.getClass().getSimpleName()
            ));
        }
    }

    private boolean isSetter(Method setter) {
        String name = setter.getName();
        return name.length() > 3 && name.startsWith("set")
                && void.class.isAssignableFrom(setter.getReturnType());
    }

    private Object getBean(ConfigurableBeanFactory factory, Class<?> type) {
        if (!factory.containsBean(type)) {
            BeanDefinition bd = factory.getBeanDefinition(type);
            return factory.createBean(bd.getType().getSimpleName(), bd);
        } else {
            return factory.getBean(type);
        }
    }

    private void inject(Object bean, Object inject, Method setter) {
        inject(bean, inject, () -> {
            try {
                setter.invoke(bean, inject);
            } catch (InvocationTargetException e) {
                throw new BeanInstantiationException(String.format("Method '%s' threw an exception.", setter.getName()), e);
            }
        });
    }

    private void inject(Object bean, Object inject, Field f) {
        inject(bean, inject, () -> {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            f.set(bean, inject);
        });
    }

    private void inject(Object bean, Object inject, InjectionCallback callback) {
        try {
            callback.inject();
        } catch (IllegalAccessException e) {
            throw new BeanInstantiationException(String.format("Failed to inject '%s' to '%s'.", inject, bean), e);
        }
    }

    private Method findSetter(Object bean, Field f) {
        String setterName = String.format("set%s", firstToUppercase(f.getName()));
        return Arrays.stream(bean.getClass().getMethods()).
                filter(m -> setterName.equals(m.getName())).
                filter(m -> m.getParameterCount() == 0).
                findFirst().orElse(null);
    }

    private String firstToUppercase(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @FunctionalInterface
    private interface InjectionCallback {
        void inject() throws IllegalAccessException;
    }
}

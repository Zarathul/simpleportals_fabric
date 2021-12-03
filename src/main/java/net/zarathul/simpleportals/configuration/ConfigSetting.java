package net.zarathul.simpleportals.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigSetting
{
	String description() default "";
	String descriptionKey();
	String category() default "";
	boolean needsWorldRestart() default false;
	int permissionLvl() default 0;
	boolean clientOnly() default false;
}

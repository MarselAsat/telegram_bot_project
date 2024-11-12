package ru.telproject.utils;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.telproject.service.custom_interface.Command;

import java.lang.reflect.Method;

@UtilityClass
public class CommandUtils {
    public boolean hasExecuteNextMessageImplementation(Command command){
        try{
            Method method = command.getClass().getMethod("executeNextMessage", Message.class);
            return !method.isDefault();
        }catch (NoSuchMethodException e){
            return false;
        }
    }
}

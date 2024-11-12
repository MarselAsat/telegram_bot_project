package ru.telproject.service.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.telproject.service.custom_interface.Command;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommandPull {
    private final ViewRecordCommand viewRecordCommand;
    private final CreateRecordCommand createRecordCommand;
    private final DeleteRecordCommand deleteRecordCommand;
    private final CreateTypeRecordCommand createTypeRecordCommand;
    private final UpdateRecordCommand updateRecordCommand;
    private final DeleteTypeRecordCommand deleteTypeRecordCommand;
    private final InfoViewCommand infoViewCommand;
    private final FirstRegistrationCommand firstRegistrationCommand;
    private final ViewTypeRecordCommand viewTypeRecordCommand;
    private Map<String, Command> commands = new HashMap<>();

    @PostConstruct
    public void init(){
        commands.put("view_records", viewRecordCommand);
        commands.put("create_record", createRecordCommand);
        commands.put("delete_record", deleteRecordCommand);
        commands.put("update_record", updateRecordCommand);
        commands.put("create_type_record", createTypeRecordCommand);
        commands.put("delete_type_record", deleteTypeRecordCommand);
        commands.put("info", infoViewCommand);
        commands.put("first_registration_command", firstRegistrationCommand);
        commands.put("view_type_records", viewTypeRecordCommand);
    }

    public Command getCommand(String command){
        return commands.get(command);
    }
}

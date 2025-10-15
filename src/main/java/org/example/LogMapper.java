package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogMapper {


    public List<Logs> mapearLogs(InputStream inputStream) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Logs> logsDoJson = objectMapper.readValue(
                    inputStream, new TypeReference<List<Logs>>() {
                    });
            return logsDoJson;


        }




}

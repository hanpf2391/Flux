
package com.flux.entropia.controller;

import com.flux.entropia.entity.Message;
import com.flux.entropia.mapper.MessageMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/debug")
@Profile("dev") // Ensure this controller is only active during development
public class DebugController {

    private final MessageMapper messageMapper;
    private final Random random = new Random();

    public DebugController(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * Generates a specified number of random messages for testing purposes.
     * This endpoint should only be enabled in a 'dev' profile.
     * @param count The number of messages to generate. Defaults to 1000.
     * @return A confirmation message.
     */
    @GetMapping("/generate-data")
    public String generateData(@RequestParam(defaultValue = "1000") int count) {
        if (count <= 0 || count > 10000) { // Safety limit
            return "Count must be between 1 and 10,000.";
        }

        List<Message> messages = new ArrayList<>();
        int worldSize = 10000; // The size of the world canvas to spread messages across

        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setContent("Message #" + i);
            // Distribute messages randomly around the origin
            message.setRowIndex(random.nextInt(worldSize) - worldSize / 2);
            message.setColIndex(random.nextInt(worldSize) - worldSize / 2);
            message.setIpAddress("127.0.0.1");
            messages.add(message);
        }

        // Looping is acceptable for a one-time debug utility.
        // We wrap in a try-catch to gracefully handle potential duplicate key errors
        // if the random generator produces the same coordinates twice.
        int insertedCount = 0;
        for (Message message : messages) {
            try {
                messageMapper.insert(message);
                insertedCount++;
            } catch (Exception e) {
                // Ignore duplicate key exceptions, just don't increment the counter
            }
        }

        return String.format("Successfully generated and inserted %d messages.", insertedCount);
    }
}

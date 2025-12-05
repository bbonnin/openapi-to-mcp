package io.openapimcp.example.assistant;

import java.util.Scanner;

public class InfraAssistantApp {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java InfraAssistantApp <ollama url> <mcp server url>");
            System.exit(1);
        }

        InfraAssistant assistant = InfraAssistantFactory.createAssistant(args[0], args[1]);

        System.out.println("You can chat with the assistant, who will be able to help you as best as they can");
        System.out.println("Example: 'Give me the list of the VM in my infrastructure. If a VM is down, start it.'");

        String input = readInput();

        while (!"bye".equalsIgnoreCase(input)) {
            String answer = assistant.chat(input);
            System.out.println("\u001B[33m" + answer + "\u001B[37m");
            input = readInput();
        }
    }

    private static String readInput() {
        Scanner in = new Scanner(System.in);
        System.out.print("> ");
        return in.nextLine();
    }
}

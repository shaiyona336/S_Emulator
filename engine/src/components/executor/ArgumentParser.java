package components.executor;

import java.util.ArrayList;
import java.util.List;

public class ArgumentParser {

    public static List<String> parseArguments(String fullArgString) {
        List<String> arguments = new ArrayList<>();

        //if the string is empty or whitespace only, return empty list
        if (fullArgString == null || fullArgString.trim().isEmpty()) {
            return arguments;
        }

        int parenthesisDepth = 0;
        int lastSplitIndex = 0;

        for (int i = 0; i < fullArgString.length(); i++) {
            char c = fullArgString.charAt(i);
            if (c == '(') {
                parenthesisDepth++;
            } else if (c == ')') {
                parenthesisDepth--;
            } else if (c == ',' && parenthesisDepth == 0) {
                // We've found a top-level comma, so we split here
                String arg = fullArgString.substring(lastSplitIndex, i).trim();
                if (!arg.isEmpty()) {  // Only add non-empty arguments
                    arguments.add(arg);
                }
                lastSplitIndex = i + 1;
            }
        }

        //add the last argument only if it's not empty
        String lastArg = fullArgString.substring(lastSplitIndex).trim();
        if (!lastArg.isEmpty()) {
            arguments.add(lastArg);
        }

        return arguments;
    }
}
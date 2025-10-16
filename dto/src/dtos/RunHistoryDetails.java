package dtos;

import java.io.Serializable;
import java.util.List;

public record RunHistoryDetails(
        int runNumber,
        int expansionDegree,
        List<Long> inputs,
        Long yValue,
        int cyclesNumber,
        String programName,
        String programType,
        String architecture
) implements Serializable {

    @Override
    public String toString() {
        return "RunHistoryDetails{" +
                "runNumber=" + runNumber +
                ", expansionDegree=" + expansionDegree +
                ", yValue=" + yValue +
                ", cyclesNumber=" + cyclesNumber +
                ", programName='" + programName + '\'' +
                ", programType='" + programType + '\'' +
                ", architecture='" + architecture + '\'' +
                '}';
    }
}
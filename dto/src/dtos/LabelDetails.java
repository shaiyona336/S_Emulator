package dtos;

public record LabelDetails(String label) {
    public String getStringLabel() {
        return label;
    }
}
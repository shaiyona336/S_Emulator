package components.label;

public class FreeLabelGenerator {
    int nextSerialNumber;

    public FreeLabelGenerator(int nextSerialNumber) {
        this.nextSerialNumber = nextSerialNumber;
    }

    public Label getNextFreeLabel() {
        Label label = new StandardLabel(nextSerialNumber);
        nextSerialNumber++;
        return label;
    }
}

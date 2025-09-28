package components.label;

public enum FixedLabel implements Label {

    EXIT {
        @Override
        public String getStringLabel() {
            return "EXIT";
        }

        @Override
        public int getSerialNumber() {
            return 100;
        }
    },

    EMPTY {
        @Override
        public String getStringLabel() {
            return "";
        }

        @Override
        public int getSerialNumber() {
            return 0;
        }
    };

    @Override
    public abstract String getStringLabel();

    @Override
    public abstract int getSerialNumber();

}

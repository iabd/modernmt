package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    protected boolean leftSpaceRequired;
    protected boolean rightSpaceRequired;
    protected boolean tagRightSpaceRequired;
    private String xmlEscapedString = null;

    public Word(String placeholder) {
        super(placeholder);
        this.leftSpaceRequired = super.leftSpace != null;
        this.rightSpaceRequired = super.rightSpace != null;
        this.tagRightSpaceRequired = false;
    }

    public Word(String placeholder, String leftSpace, String rightSpace) {
        super(placeholder, leftSpace, rightSpace);
        this.leftSpaceRequired = super.leftSpace != null;
        this.rightSpaceRequired = super.rightSpace != null;
        this.tagRightSpaceRequired = false;
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace) {
        super(text, placeholder, leftSpace, rightSpace);
        this.leftSpaceRequired = super.leftSpace != null;
        this.rightSpaceRequired = super.rightSpace != null;
        this.tagRightSpaceRequired = false;
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace, boolean leftSpaceRequired, boolean rightSpaceRequired) {
        super(text, placeholder, leftSpace, rightSpace);
        this.leftSpaceRequired = leftSpaceRequired;
        this.rightSpaceRequired = rightSpaceRequired;
        this.tagRightSpaceRequired = false;
    }

    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired, boolean tagRightSpaceRequired) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = rightSpaceRequired;
        this.tagRightSpaceRequired = tagRightSpaceRequired;
    }


    public boolean isLeftSpaceRequired() { return leftSpaceRequired; }

    public boolean isRightSpaceRequired() { return rightSpaceRequired; }

    public void setLeftSpaceRequired(boolean leftSpaceRequired) {
        this.leftSpaceRequired = leftSpaceRequired;
    }
    public boolean isTagRightSpaceRequired() {
        return tagRightSpaceRequired;
    }

    public void setRightSpaceRequired(boolean rightSpaceRequired) {
        this.rightSpaceRequired = rightSpaceRequired;
    }
    public void setTagRightSpaceRequired(boolean tagRightSpaceRequired) { this.tagRightSpaceRequired = tagRightSpaceRequired; }

    @Override
    public void setText(String text) {
        super.setText(text);
        xmlEscapedString = null;
    }

    @Override
    public void setPlaceholder(String placeholder) {
        super.setPlaceholder(placeholder);
        xmlEscapedString = null;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean escape) {
        if (escape) {
            if (xmlEscapedString == null)
                xmlEscapedString = XMLUtils.escapeText(super.toString());

            return xmlEscapedString;
        } else {
            return super.toString();
        }
    }

}

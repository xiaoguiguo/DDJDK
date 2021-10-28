package java.lang;

import java.io.Serializable;

public class Throwable implements Serializable {
    private static final long serialVersionUID = -5483213135545696013L;

    private transient Object backtrace;

    private String detailMessage;


}

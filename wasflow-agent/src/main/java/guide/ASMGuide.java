package guide;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ASMGuide {

    public int add(int a, int b) {
        return a + b;
    }

    public int addModify(int a, int b) {
        guide.CollectGuide.addCalled(a, b);
        return a + b;
    }
}
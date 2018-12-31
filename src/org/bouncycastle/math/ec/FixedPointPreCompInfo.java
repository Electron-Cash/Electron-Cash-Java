package org.bouncycastle.math.ec;


public class FixedPointPreCompInfo implements PreCompInfo
{
  
    protected ECPoint[] preComp = null;


    protected int width = -1;

    public ECPoint[] getPreComp()
    {
        return preComp;
    }

    public void setPreComp(ECPoint[] preComp)
    {
        this.preComp = preComp;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
}

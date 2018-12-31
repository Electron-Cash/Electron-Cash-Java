package org.bouncycastle.crypto.tls;

public class ECBasisType
{
    public static final short ec_basis_trinomial = 1;
    public static final short ec_basis_pentanomial = 2;

    public static boolean isValid(short ecBasisType)
    {
        return ecBasisType >= ec_basis_trinomial && ecBasisType <= ec_basis_pentanomial;
    }
}

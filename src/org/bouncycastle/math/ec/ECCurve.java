package org.bouncycastle.math.ec;

import java.util.Hashtable;
import java.util.Random;

import org.bouncycastle.math.ec.endo.ECEndomorphism;
import org.bouncycastle.math.ec.endo.GLVEndomorphism;
import org.bouncycastle.math.field.FiniteField;
import org.bouncycastle.math.field.FiniteFields;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.Integers;
import org.electroncash.util.BigInteger;

public abstract class ECCurve {
	public static final int COORD_AFFINE = 0;
	public static final int COORD_HOMOGENEOUS = 1;
	public static final int COORD_JACOBIAN = 2;
	public static final int COORD_JACOBIAN_CHUDNOVSKY = 3;
	public static final int COORD_JACOBIAN_MODIFIED = 4;
	public static final int COORD_LAMBDA_AFFINE = 5;
	public static final int COORD_LAMBDA_PROJECTIVE = 6;
	public static final int COORD_SKEWED = 7;

	public static int[] getAllCoordinateSystems() {
		return new int[] { COORD_AFFINE, COORD_HOMOGENEOUS, COORD_JACOBIAN, COORD_JACOBIAN_CHUDNOVSKY,
				COORD_JACOBIAN_MODIFIED, COORD_LAMBDA_AFFINE, COORD_LAMBDA_PROJECTIVE, COORD_SKEWED };
	}

	public class Config {
		protected int coord;
		protected ECEndomorphism endomorphism;
		protected ECMultiplier multiplier;

		Config(int coord, ECEndomorphism endomorphism, ECMultiplier multiplier) {
			this.coord = coord;
			this.endomorphism = endomorphism;
			this.multiplier = multiplier;
		}

		public Config setCoordinateSystem(int coord) {
			this.coord = coord;
			return this;
		}

		public Config setEndomorphism(ECEndomorphism endomorphism) {
			this.endomorphism = endomorphism;
			return this;
		}

		public Config setMultiplier(ECMultiplier multiplier) {
			this.multiplier = multiplier;
			return this;
		}

		public ECCurve create() {
			if (!supportsCoordinateSystem(coord)) {
				throw new IllegalStateException("unsupported coordinate system");
			}

			ECCurve c = cloneCurve();
			if (c == ECCurve.this) {
				throw new IllegalStateException("implementation returned current curve");
			}

			c.coord = coord;
			c.endomorphism = endomorphism;
			c.multiplier = multiplier;

			return c;
		}
	}

	protected FiniteField field;
	protected ECFieldElement a, b;
	protected BigInteger order, cofactor;

	protected int coord = COORD_AFFINE;
	protected ECEndomorphism endomorphism = null;
	protected ECMultiplier multiplier = null;

	protected ECCurve(FiniteField field) {
		this.field = field;
	}

	public abstract int getFieldSize();

	public abstract ECFieldElement fromBigInteger(BigInteger x);

	public Config configure() {
		return new Config(this.coord, this.endomorphism, this.multiplier);
	}

	public ECPoint validatePoint(BigInteger x, BigInteger y) {
		ECPoint p = createPoint(x, y);
		if (!p.isValid()) {
			throw new IllegalArgumentException("Invalid point coordinates");
		}
		return p;
	}

	public ECPoint validatePoint(BigInteger x, BigInteger y, boolean withCompression) {
		ECPoint p = createPoint(x, y, withCompression);
		if (!p.isValid()) {
			throw new IllegalArgumentException("Invalid point coordinates");
		}
		return p;
	}

	public ECPoint createPoint(BigInteger x, BigInteger y) {
		return createPoint(x, y, false);
	}

	public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression) {
		return createRawPoint(fromBigInteger(x), fromBigInteger(y), withCompression);
	}

	protected abstract ECCurve cloneCurve();

	protected abstract ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression);

	protected abstract ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs,
			boolean withCompression);

	protected ECMultiplier createDefaultMultiplier() {
		if (endomorphism instanceof GLVEndomorphism) {
			return new GLVMultiplier(this, (GLVEndomorphism) endomorphism);
		}

		return new WNafL2RMultiplier();
	}

	public boolean supportsCoordinateSystem(int coord) {
		return coord == COORD_AFFINE;
	}

	public PreCompInfo getPreCompInfo(ECPoint point, String name) {
		checkPoint(point);
		synchronized (point) {
			Hashtable table = point.preCompTable;
			return table == null ? null : (PreCompInfo) table.get(name);
		}
	}

	public void setPreCompInfo(ECPoint point, String name, PreCompInfo preCompInfo) {
		checkPoint(point);
		synchronized (point) {
			Hashtable table = point.preCompTable;
			if (null == table) {
				point.preCompTable = table = new Hashtable(4);
			}
			table.put(name, preCompInfo);
		}
	}

	public ECPoint importPoint(ECPoint p) {
		if (this == p.getCurve()) {
			return p;
		}
		if (p.isInfinity()) {
			return getInfinity();
		}

		p = p.normalize();

		return validatePoint(p.getXCoord().toBigInteger(), p.getYCoord().toBigInteger(), p.withCompression);
	}

	public void normalizeAll(ECPoint[] points) {
		checkPoints(points);

		if (this.getCoordinateSystem() == ECCurve.COORD_AFFINE) {
			return;
		}

		ECFieldElement[] zs = new ECFieldElement[points.length];
		int[] indices = new int[points.length];
		int count = 0;
		for (int i = 0; i < points.length; ++i) {
			ECPoint p = points[i];
			if (null != p && !p.isNormalized()) {
				zs[count] = p.getZCoord(0);
				indices[count++] = i;
			}
		}

		if (count == 0) {
			return;
		}

		ECAlgorithms.montgomeryTrick(zs, 0, count);

		for (int j = 0; j < count; ++j) {
			int index = indices[j];
			points[index] = points[index].normalize(zs[j]);
		}
	}

	public abstract ECPoint getInfinity();

	public FiniteField getField() {
		return field;
	}

	public ECFieldElement getA() {
		return a;
	}

	public ECFieldElement getB() {
		return b;
	}

	public BigInteger getOrder() {
		return order;
	}

	public BigInteger getCofactor() {
		return cofactor;
	}

	public int getCoordinateSystem() {
		return coord;
	}

	protected abstract ECPoint decompressPoint(int yTilde, BigInteger X1);

	public ECEndomorphism getEndomorphism() {
		return endomorphism;
	}

	public synchronized ECMultiplier getMultiplier() {
		if (this.multiplier == null) {
			this.multiplier = createDefaultMultiplier();
		}
		return this.multiplier;
	}

	public ECPoint decodePoint(byte[] encoded) {
		ECPoint p = null;
		int expectedLength = (getFieldSize() + 7) / 8;

		byte type = encoded[0];
		switch (type) {
		case 0x00: {
			if (encoded.length != 1) {
				throw new IllegalArgumentException("Incorrect length for infinity encoding");
			}

			p = getInfinity();
			break;
		}
		case 0x02:
		case 0x03: {
			if (encoded.length != (expectedLength + 1)) {
				throw new IllegalArgumentException("Incorrect length for compressed encoding");
			}

			int yTilde = type & 1;
			BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);

			p = decompressPoint(yTilde, X);
			if (!p.satisfiesCofactor()) {
				throw new IllegalArgumentException("Invalid point");
			}

			break;
		}
		case 0x04: {
			if (encoded.length != (2 * expectedLength + 1)) {
				throw new IllegalArgumentException("Incorrect length for uncompressed encoding");
			}

			BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
			BigInteger Y = BigIntegers.fromUnsignedByteArray(encoded, 1 + expectedLength, expectedLength);

			p = validatePoint(X, Y);
			break;
		}
		case 0x06:
		case 0x07: {
			if (encoded.length != (2 * expectedLength + 1)) {
				throw new IllegalArgumentException("Incorrect length for hybrid encoding");
			}

			BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
			BigInteger Y = BigIntegers.fromUnsignedByteArray(encoded, 1 + expectedLength, expectedLength);

			if (Y.testBit(0) != (type == 0x07)) {
				throw new IllegalArgumentException("Inconsistent Y coordinate in hybrid encoding");
			}

			p = validatePoint(X, Y);
			break;
		}
		default:
			throw new IllegalArgumentException("Invalid point encoding 0x" + Integer.toString(type, 16));
		}

		if (type != 0x00 && p.isInfinity()) {
			throw new IllegalArgumentException("Invalid infinity encoding");
		}

		return p;
	}

	protected void checkPoint(ECPoint point) {
		if (null == point || (this != point.getCurve())) {
			throw new IllegalArgumentException("'point' must be non-null and on this curve");
		}
	}

	protected void checkPoints(ECPoint[] points) {
		if (points == null) {
			throw new IllegalArgumentException("'points' cannot be null");
		}

		for (int i = 0; i < points.length; ++i) {
			ECPoint point = points[i];
			if (null != point && this != point.getCurve()) {
				throw new IllegalArgumentException("'points' entries must be null or on this curve");
			}
		}
	}

	public boolean equals(ECCurve other) {
		return this == other || (null != other && getField().equals(other.getField())
				&& getA().toBigInteger().equals(other.getA().toBigInteger())
				&& getB().toBigInteger().equals(other.getB().toBigInteger()));
	}

	public boolean equals(Object obj) {
		return this == obj || (obj instanceof ECCurve && equals((ECCurve) obj));
	}

	public int hashCode() {
		return getField().hashCode() ^ Integers.rotateLeft(getA().toBigInteger().hashCode(), 8)
				^ Integers.rotateLeft(getB().toBigInteger().hashCode(), 16);
	}

	public static abstract class AbstractFp extends ECCurve {
		protected AbstractFp(BigInteger q) {
			super(FiniteFields.getPrimeField(q));
		}

		protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
			ECFieldElement x = this.fromBigInteger(X1);
			ECFieldElement rhs = x.square().add(a).multiply(x).add(b);
			ECFieldElement y = rhs.sqrt();

			if (y == null) {
				throw new IllegalArgumentException("Invalid point compression");
			}

			if (y.testBitZero() != (yTilde == 1)) {
				y = y.negate();
			}

			return this.createRawPoint(x, y, true);
		}
	}

	public static class Fp extends AbstractFp {
		private static final int FP_DEFAULT_COORDS = COORD_JACOBIAN_MODIFIED;

		BigInteger q, r;
		ECPoint.Fp infinity;

		public Fp(BigInteger q, BigInteger a, BigInteger b) {
			this(q, a, b, null, null);
		}

		public Fp(BigInteger q, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
			super(q);

			this.q = q;
			this.r = ECFieldElement.Fp.calculateResidue(q);
			this.infinity = new ECPoint.Fp(this, null, null);

			this.a = fromBigInteger(a);
			this.b = fromBigInteger(b);
			this.order = order;
			this.cofactor = cofactor;
			this.coord = FP_DEFAULT_COORDS;
		}

		protected Fp(BigInteger q, BigInteger r, ECFieldElement a, ECFieldElement b) {
			this(q, r, a, b, null, null);
		}

		protected Fp(BigInteger q, BigInteger r, ECFieldElement a, ECFieldElement b, BigInteger order,
				BigInteger cofactor) {
			super(q);

			this.q = q;
			this.r = r;
			this.infinity = new ECPoint.Fp(this, null, null);

			this.a = a;
			this.b = b;
			this.order = order;
			this.cofactor = cofactor;
			this.coord = FP_DEFAULT_COORDS;
		}

		protected ECCurve cloneCurve() {
			return new Fp(q, r, a, b, order, cofactor);
		}

		public boolean supportsCoordinateSystem(int coord) {
			switch (coord) {
			case COORD_AFFINE:
			case COORD_HOMOGENEOUS:
			case COORD_JACOBIAN:
			case COORD_JACOBIAN_MODIFIED:
				return true;
			default:
				return false;
			}
		}

		public BigInteger getQ() {
			return q;
		}

		public int getFieldSize() {
			return q.bitLength();
		}

		public ECFieldElement fromBigInteger(BigInteger x) {
			return new ECFieldElement.Fp(this.q, this.r, x);
		}

		protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression) {
			return new ECPoint.Fp(this, x, y, withCompression);
		}

		protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs,
				boolean withCompression) {
			return new ECPoint.Fp(this, x, y, zs, withCompression);
		}

		public ECPoint importPoint(ECPoint p) {
			if (this != p.getCurve() && this.getCoordinateSystem() == COORD_JACOBIAN && !p.isInfinity()) {
				switch (p.getCurve().getCoordinateSystem()) {
				case COORD_JACOBIAN:
				case COORD_JACOBIAN_CHUDNOVSKY:
				case COORD_JACOBIAN_MODIFIED:
					return new ECPoint.Fp(this, fromBigInteger(p.x.toBigInteger()), fromBigInteger(p.y.toBigInteger()),
							new ECFieldElement[] { fromBigInteger(p.zs[0].toBigInteger()) }, p.withCompression);
				default:
					break;
				}
			}

			return super.importPoint(p);
		}

		public ECPoint getInfinity() {
			return infinity;
		}
	}

	public static abstract class AbstractF2m extends ECCurve {
		private static FiniteField buildField(int m, int k1, int k2, int k3) {
			if (k1 == 0) {
				throw new IllegalArgumentException("k1 must be > 0");
			}

			if (k2 == 0) {
				if (k3 != 0) {
					throw new IllegalArgumentException("k3 must be 0 if k2 == 0");
				}

				return FiniteFields.getBinaryExtensionField(new int[] { 0, k1, m });
			}

			if (k2 <= k1) {
				throw new IllegalArgumentException("k2 must be > k1");
			}

			if (k3 <= k2) {
				throw new IllegalArgumentException("k3 must be > k2");
			}

			return FiniteFields.getBinaryExtensionField(new int[] { 0, k1, k2, k3, m });
		}

		protected AbstractF2m(int m, int k1, int k2, int k3) {
			super(buildField(m, k1, k2, k3));
		}
	}

	public static class F2m extends AbstractF2m {
		private static final int F2M_DEFAULT_COORDS = COORD_LAMBDA_PROJECTIVE;

		private int m;

		private int k1;

		private int k2;

		private int k3;

		private ECPoint.F2m infinity;

		private byte mu = 0;

		private BigInteger[] si = null;

		public F2m(int m, int k, BigInteger a, BigInteger b) {
			this(m, k, 0, 0, a, b, null, null);
		}

		public F2m(int m, int k, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
			this(m, k, 0, 0, a, b, order, cofactor);
		}

		public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b) {
			this(m, k1, k2, k3, a, b, null, null);
		}

		public F2m(int m, int k1, int k2, int k3, BigInteger a, BigInteger b, BigInteger order, BigInteger cofactor) {
			super(m, k1, k2, k3);

			this.m = m;
			this.k1 = k1;
			this.k2 = k2;
			this.k3 = k3;
			this.order = order;
			this.cofactor = cofactor;

			this.infinity = new ECPoint.F2m(this, null, null);
			this.a = fromBigInteger(a);
			this.b = fromBigInteger(b);
			this.coord = F2M_DEFAULT_COORDS;
		}

		protected F2m(int m, int k1, int k2, int k3, ECFieldElement a, ECFieldElement b, BigInteger order,
				BigInteger cofactor) {
			super(m, k1, k2, k3);

			this.m = m;
			this.k1 = k1;
			this.k2 = k2;
			this.k3 = k3;
			this.order = order;
			this.cofactor = cofactor;

			this.infinity = new ECPoint.F2m(this, null, null);
			this.a = a;
			this.b = b;
			this.coord = F2M_DEFAULT_COORDS;
		}

		protected ECCurve cloneCurve() {
			return new F2m(m, k1, k2, k3, a, b, order, cofactor);
		}

		public boolean supportsCoordinateSystem(int coord) {
			switch (coord) {
			case COORD_AFFINE:
			case COORD_HOMOGENEOUS:
			case COORD_LAMBDA_PROJECTIVE:
				return true;
			default:
				return false;
			}
		}

		protected ECMultiplier createDefaultMultiplier() {
			if (isKoblitz()) {
				return new WTauNafMultiplier();
			}

			return super.createDefaultMultiplier();
		}

		public int getFieldSize() {
			return m;
		}

		public ECFieldElement fromBigInteger(BigInteger x) {
			return new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3, x);
		}

		public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression) {
			ECFieldElement X = fromBigInteger(x), Y = fromBigInteger(y);

			switch (this.getCoordinateSystem()) {
			case COORD_LAMBDA_AFFINE:
			case COORD_LAMBDA_PROJECTIVE: {
				if (X.isZero()) {
					if (!Y.square().equals(this.getB())) {
						throw new IllegalArgumentException();
					}
				} else {
					Y = Y.divide(X).add(X);
				}
				break;
			}
			default: {
				break;
			}
			}

			return createRawPoint(X, Y, withCompression);
		}

		protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression) {
			return new ECPoint.F2m(this, x, y, withCompression);
		}

		protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, ECFieldElement[] zs,
				boolean withCompression) {
			return new ECPoint.F2m(this, x, y, zs, withCompression);
		}

		public ECPoint getInfinity() {
			return infinity;
		}

		public boolean isKoblitz() {
			return order != null && cofactor != null && b.isOne() && (a.isZero() || a.isOne());
		}

		synchronized byte getMu() {
			if (mu == 0) {
				mu = Tnaf.getMu(this);
			}
			return mu;
		}

		synchronized BigInteger[] getSi() {
			if (si == null) {
				si = Tnaf.getSi(this);
			}
			return si;
		}

		protected ECPoint decompressPoint(int yTilde, BigInteger X1) {
			ECFieldElement x = fromBigInteger(X1), y = null;
			if (x.isZero()) {
				y = b.sqrt();
			} else {
				ECFieldElement beta = x.square().invert().multiply(b).add(a).add(x);
				ECFieldElement z = solveQuadraticEquation(beta);
				if (z != null) {
					if (z.testBitZero() != (yTilde == 1)) {
						z = z.addOne();
					}

					switch (this.getCoordinateSystem()) {
					case COORD_LAMBDA_AFFINE:
					case COORD_LAMBDA_PROJECTIVE: {
						y = z.add(x);
						break;
					}
					default: {
						y = z.multiply(x);
						break;
					}
					}
				}
			}

			if (y == null) {
				throw new IllegalArgumentException("Invalid point compression");
			}

			return this.createRawPoint(x, y, true);
		}

		private ECFieldElement solveQuadraticEquation(ECFieldElement beta) {
			if (beta.isZero()) {
				return beta;
			}

			ECFieldElement zeroElement = fromBigInteger(ECConstants.ZERO);

			ECFieldElement z = null;
			ECFieldElement gamma = null;

			Random rand = new Random();
			do {
				ECFieldElement t = fromBigInteger(new BigInteger(m, rand));
				z = zeroElement;
				ECFieldElement w = beta;
				for (int i = 1; i <= m - 1; i++) {
					ECFieldElement w2 = w.square();
					z = z.square().add(w2.multiply(t));
					w = w2.add(beta);
				}
				if (!w.isZero()) {
					return null;
				}
				gamma = z.square().add(z);
			} while (gamma.isZero());

			return z;
		}

		public int getM() {
			return m;
		}

		public boolean isTrinomial() {
			return k2 == 0 && k3 == 0;
		}

		public int getK1() {
			return k1;
		}

		public int getK2() {
			return k2;
		}

		public int getK3() {
			return k3;
		}

		public BigInteger getN() {
			return order;
		}

		public BigInteger getH() {
			return cofactor;
		}
	}
}

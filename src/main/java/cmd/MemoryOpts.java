package cmd;

import util.LogBuffer;

public enum MemoryOpts {
	SIXTEENG("16 Gigs"),EIGHTG("8 Gigs"),FOURG("4 Gigs"),THREEG("3 Gigs"),
	TWOG("2 Gigs"),ONEG("1 Gig"),FIVEHM("500 Megs"),TWOFIFTYM("250 Megs"),
	ONEHM("100 megs");

	private final String toString;

	private MemoryOpts(String toString) {
		this.toString = toString;
	}

	@Override
	public String toString() {
		return toString;
	}

	public String toJVMFlag() {
		if(this == MemoryOpts.SIXTEENG) {
			return("-Xmx16g");
		} else if(this == MemoryOpts.EIGHTG) {
			return("-Xmx8g");
		} else if(this == MemoryOpts.FOURG) {
			return("-Xmx4g");
		} else if(this == MemoryOpts.THREEG) {
			return("-Xmx3g");
		} else if(this == MemoryOpts.TWOG) {
			return("-Xmx2g");
		} else if(this == MemoryOpts.ONEG) {
			return("-Xmx1g");
		} else if(this == MemoryOpts.FIVEHM) {
			return("-Xmx500M");
		} else if(this == MemoryOpts.TWOFIFTYM) {
			return("-Xmx250M");
		} else if(this == MemoryOpts.ONEHM) {
			return("-Xmx100M");
		}
		return(null);
	}

	public long toLongKb() {
		if(this == MemoryOpts.SIXTEENG) {
			return(15275655);
		} else if(this == MemoryOpts.EIGHTG) {
			return(7637827);
		} else if(this == MemoryOpts.FOURG) {
			return(3818913);
		} else if(this == MemoryOpts.THREEG) {
			return(2864185);
		} else if(this == MemoryOpts.TWOG) {
			return(1909456);
		} else if(this == MemoryOpts.ONEG) {
			return(954728);
		} else if(this == MemoryOpts.FIVEHM) {
			return(477364);
		} else if(this == MemoryOpts.TWOFIFTYM) {
			return(238682);
		} else if(this == MemoryOpts.ONEHM) {
			return(95472);
		}
		return(0);
	}

	/**
	 * This returns the closest option larger than the one supplied (plus the
	 * withinkb value).  Based on:
	 * https://stackoverflow.com/questions/23701207/why-do-xmx-and-runtime-
	 * maxmemory-not-agree
	 * 
	 * @return
	 */
	public static MemoryOpts getNextOptUp(final long withinkb,
		final String prev_xmx) {

		final long curmax = getCurrentMaxInKB();
		MemoryOpts[] opts = MemoryOpts.getAvailable(withinkb,prev_xmx);
		MemoryOpts closestopt = getLargest();
		long closest = closestopt.toLongKb();
		for(MemoryOpts opt : opts) {
			if(opt.toLongKb() >= (curmax + withinkb) &&
				Math.abs(opt.toLongKb() - curmax) <
				Math.abs(opt.toLongKb() - closest)) {

				closest = opt.toLongKb();
				closestopt = opt;
			}
		}
		LogBuffer.println("Current max memory: [" + curmax +
			"].  Next memory option up: [" + closestopt.toLongKb() + "].");
		return(closestopt);
	}

	public static long getCurrentMaxInKB() {
		return(Runtime.getRuntime().maxMemory() / 1000);
	}

	public static MemoryOpts getLargest() {
		return(MemoryOpts.SIXTEENG);
	}

	public static MemoryOpts getSmallest() {
		return(MemoryOpts.ONEHM);
	}

	/**
	 * This returns the memory options for *increasing* the memory and
	 * restarting.  In other words, it doesn't return anything less than or
	 * equal to the current max (-Xmx).
	 * 
	 * @return
	 */
	public static MemoryOpts[] getAvailable(final long withinkb,
		final String prev_xmx) {

		MemoryOpts cur_xmx = xmxToMemOpt(prev_xmx);

		if(cur_xmx == null) {
			long max = getCurrentMaxInKB();
	
			MemoryOpts[] opts = MemoryOpts.values();
			MemoryOpts closestopt = getLargest();
			long closest = closestopt.toLongKb();
			for(MemoryOpts opt : opts) {
				if(opt.toLongKb() > (max + withinkb) &&
					Math.abs(opt.toLongKb() - max) <
					Math.abs(opt.toLongKb() - closest)) {
					closestopt = opt;
					closest = closestopt.toLongKb();
				}
			}
			if(closestopt != null) {
				return(getEqualAndLarger(closestopt));
			}
			MemoryOpts[] none = {};
			return(none);
		} else {
			return(getLarger(cur_xmx));
		}
	}

	public static MemoryOpts xmxToMemOpt(final String xmx_str) {
		if(xmx_str == "") {
			return(null);
		}
		MemoryOpts[] opts = MemoryOpts.values();
		for(MemoryOpts opt : opts) {
			if(opt.toJVMFlag() == xmx_str) {
				return(opt);
			}
		}
		return(null);
	}

	public static MemoryOpts[] getEqualAndLarger(MemoryOpts opt) {
		if(opt == ONEHM) {
			MemoryOpts[] eal = MemoryOpts.values();
			return(eal);
		} else if(opt == TWOFIFTYM) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG,ONEG,FIVEHM,
				TWOFIFTYM};
			return(eal);
		} else if(opt == FIVEHM) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG,ONEG,FIVEHM};
			return(eal);
		} else if(opt == ONEG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG,ONEG};
			return(eal);
		} else if(opt == TWOG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG};
			return(eal);
		} else if(opt == THREEG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG};
			return(eal);
		} else if(opt == FOURG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG};
			return(eal);
		} else if(opt == EIGHTG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG};
			return(eal);
		} else if(opt == SIXTEENG) {
			MemoryOpts[] eal = {SIXTEENG};
			return(eal);
		}

		MemoryOpts[] none = {};
		return(none);
	}

	public static MemoryOpts[] getLarger(MemoryOpts opt) {
		if(opt == ONEHM) {
			MemoryOpts[] eal = MemoryOpts.values();
			return(eal);
		} else if(opt == TWOFIFTYM) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG,ONEG,FIVEHM};
			return(eal);
		} else if(opt == FIVEHM) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG,ONEG};
			return(eal);
		} else if(opt == ONEG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG,TWOG};
			return(eal);
		} else if(opt == TWOG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG,THREEG};
			return(eal);
		} else if(opt == THREEG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG,FOURG};
			return(eal);
		} else if(opt == FOURG) {
			MemoryOpts[] eal = {SIXTEENG,EIGHTG};
			return(eal);
		} else if(opt == EIGHTG) {
			MemoryOpts[] eal = {SIXTEENG};
			return(eal);
		}

		MemoryOpts[] none = {};
		return(none);
	}

	public static MemoryOpts getDefault() {
		return(MemoryOpts.TWOG);
	}
}

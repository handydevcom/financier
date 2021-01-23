/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding default currency and fromCursor
 ******************************************************************************/
package com.handydev.financier.model;

import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.utils.CurrencyCache;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import static com.handydev.financier.db.DatabaseHelper.CURRENCY_TABLE;
import static com.handydev.orb.EntityManager.DEF_SORT_COL;

@Entity
@Table(name = CURRENCY_TABLE)
public class Currency extends MyEntity implements SortableEntity {

	public static final Currency EMPTY = new Currency();
	
	static {
        EMPTY.id = 0;
        EMPTY.name = "";
        EMPTY.title = "Default";
		EMPTY.symbol = "";
        EMPTY.symbolFormat = SymbolFormat.RS;
		EMPTY.decimals = 2;
        EMPTY.decimalSeparator = "'.'";
        EMPTY.groupSeparator = "','";
	}

	@Column(name = "name")
	public String name;

	@Column(name = "symbol")
	public String symbol;

    @Column(name = "symbol_format")
    public SymbolFormat symbolFormat = SymbolFormat.RS;

	@Column(name = "is_default")
	public boolean isDefault;

	@Column(name = "decimals")
	public int decimals = 2;

	@Column(name = "decimal_separator")
	public String decimalSeparator;

	@Column(name = "group_separator")
	public String groupSeparator;

	@Column(name = DEF_SORT_COL)
	public long sortOrder;

    @Transient
	private volatile DecimalFormat format;

    @Override
    public String toString() {
        return name;
    }

    public NumberFormat getFormat() {
		DecimalFormat f = format;
		if (f == null) {
			f = CurrencyCache.createCurrencyFormat(this);
			format = f;
		}
		return f;
	}
	
	public static Currency defaultCurrency() {
		Currency c = new Currency();
		c.id = 2;
		c.name = "USD";
		c.title = "American Dollar";
		c.symbol = "$";
		c.decimals = 2;
		return c;
	}

	public static @Nullable Currency getDefaultCurrency(DatabaseAdapter db) {
		List<Currency> currencies = db.getAllCurrenciesList("name");
		for (Currency currency : currencies) {
			if (currency.isDefault) {
				return currency;
			}
		}
		return null;
	}

	@Override
	public long getSortOrder() {
		return sortOrder;
	}
}

/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dsa41basis.fight;

import dsa41basis.util.HeroUtil;
import dsatool.util.Tuple;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class CloseCombatWeapon extends OffensiveWeapon {

	private final IntegerProperty at = new SimpleIntegerProperty();
	private final IntegerProperty bf = new SimpleIntegerProperty();
	private final IntegerProperty length = new SimpleIntegerProperty();
	private final StringProperty dk = new SimpleStringProperty();
	private final IntegerProperty ini = new SimpleIntegerProperty();
	private final IntegerProperty pa = new SimpleIntegerProperty();
	private final StringProperty tpkk = new SimpleStringProperty();
	private final StringProperty special = new SimpleStringProperty();
	private final StringProperty wm = new SimpleStringProperty();

	public CloseCombatWeapon(final JSONObject hero, final JSONObject weapon, final JSONObject baseWeapon, final JSONObject closeCombatTalents,
			final JSONObject actualTalents) {
		super(hero, weapon, baseWeapon, closeCombatTalents, actualTalents);
		recompute();
	}

	public final ReadOnlyIntegerProperty atProperty() {
		return at;
	}

	public final IntegerProperty bfProperty() {
		return bf;
	}

	private Tuple<Integer, Integer> computeAtPa() {
		if (hero != null && type.get() != null) {
			final int AT = HeroUtil.getAT(hero, item, type.get(), true, false, true);
			final Integer PA = HeroUtil.getPA(hero, item, type.get(), false, true);

			return new Tuple<>(AT, PA != null ? PA : Integer.MIN_VALUE);
		} else
			return new Tuple<>(0, 0);
	}

	private String computeSpecial() {
		final StringBuilder special = new StringBuilder();
		if (item.getBoolOrDefault("Improvisiert", baseItem.getBoolOrDefault("Improvisiert", false))) {
			special.append('i');
		}
		if (item.getBoolOrDefault("Zweihändig", baseItem.getBoolOrDefault("Zweihändig", false))) {
			special.append('z');
		}
		if (item.getBoolOrDefault("Privilegiert", baseItem.getBoolOrDefault("Privilegiert", false))) {
			special.append('p');
		}
		return special.toString();
	}

	public final ReadOnlyStringProperty dkProperty() {
		return dk;
	}

	public final int getAt() {
		return at.get();
	}

	public final int getBf() {
		return bf.get();
	}

	public final String getDk() {
		return dk.get();
	}

	public final int getIni() {
		return ini.get();
	}

	public final int getLength() {
		return length.get();
	}

	public final int getPa() {
		return pa.get();
	}

	public final String getSpecial() {
		return special.get();
	}

	public final String getTpkk() {
		return tpkk.get();
	}

	public final Tuple<Integer, Integer> getTpkkRaw() {
		final JSONObject TPKKValues = item.getObjOrDefault("Trefferpunkte/Körperkraft", baseItem.getObj("Trefferpunkte/Körperkraft"));
		return new Tuple<>(TPKKValues.getIntOrDefault("Schwellenwert", 1), TPKKValues.getIntOrDefault("Schadensschritte", 1));
	}

	public final String getWM() {
		return wm.get();
	}

	public Tuple<Integer, Integer> getWMraw() {
		final JSONObject weaponModifiers = item.getObjOrDefault("Waffenmodifikatoren", baseItem.getObj("Waffenmodifikatoren"));
		return new Tuple<>(weaponModifiers.getIntOrDefault("Attackemodifikator", 0), weaponModifiers.getIntOrDefault("Parademodifikator", 0));
	}

	public final IntegerProperty iniProperty() {
		return ini;
	}

	public final IntegerProperty lengthProperty() {
		return length;
	}

	public final ReadOnlyIntegerProperty paProperty() {
		return pa;
	}

	@Override
	public final void recompute() {
		super.recompute();

		final Tuple<Integer, Integer> atpa = computeAtPa();
		at.set(atpa._1);
		pa.set(atpa._2);
		ini.set(item.getIntOrDefault("Initiative:Modifikator", baseItem.getIntOrDefault("Initiative:Modifikator", 0)));

		final JSONArray distanceClasses = item.getArrOrDefault("Distanzklassen", baseItem.getArr("Distanzklassen"));
		dk.set(String.join("", distanceClasses.getStrings()));

		final Integer BF = item.getIntOrDefault("Bruchfaktor", baseItem.getInt("Bruchfaktor"));
		bf.set(BF != null ? BF : Integer.MIN_VALUE);

		length.set(item.getIntOrDefault("Länge", baseItem.getIntOrDefault("Länge", 0)));

		final JSONObject TPKKValues = item.getObjOrDefault("Trefferpunkte/Körperkraft", baseItem.getObj("Trefferpunkte/Körperkraft"));
		tpkk.set(TPKKValues.getIntOrDefault("Schwellenwert", 1).toString() + "/" + TPKKValues.getIntOrDefault("Schadensschritte", 1));

		final JSONObject weaponModifier = item.getObjOrDefault("Waffenmodifikatoren", baseItem.getObj("Waffenmodifikatoren"));
		wm.set(weaponModifier.getIntOrDefault("Attackemodifikator", 0).toString() + "/" + weaponModifier.getIntOrDefault("Parademodifikator", 0));

		special.set(computeSpecial());
	}

	@Override
	protected void recomputeAtPa() {
		final Tuple<Integer, Integer> atpa = computeAtPa();
		at.set(atpa._1);
		pa.set(atpa._2);
	}

	public final void setBf(final int bf) {
		if (bf != Integer.MIN_VALUE) {
			baseItem.put("Bruchfaktor", bf);
		} else {
			item.removeKey("Bruchfaktor");
			baseItem.removeKey("Bruchfaktor");
		}
		item.notifyListeners(null);
	}

	public final void setDK(final String dk) {
		final JSONArray distanceClasses = item.getArr("Distanzklassen");
		distanceClasses.clear();
		for (final Character c : dk.toCharArray()) {
			distanceClasses.add(c.toString());
		}
		distanceClasses.notifyListeners(null);
	}

	public final void setIni(final int ini) {
		item.put("Initiative:Modifikator", ini);
		item.notifyListeners(null);
	}

	public final void setLength(final int length) {
		baseItem.put("Länge", length);
		baseItem.notifyListeners(null);
	}

	public final void setSpecial(final boolean improvisational, final boolean twohanded, final boolean privileged) {
		if (improvisational) {
			item.put("Improvisiert", true);
		} else {
			item.removeKey("Improvisiert");
			baseItem.removeKey("Improvisiert");
		}
		if (twohanded) {
			item.put("Zweihändig", true);
		} else {
			item.removeKey("Zweihändig");
			baseItem.removeKey("Zweihändig");
		}
		if (privileged) {
			item.put("Privilegiert", true);
		} else {
			item.removeKey("Privilegiert");
			baseItem.removeKey("Privilegiert");
		}
		item.notifyListeners(null);
		baseItem.notifyListeners(null);
	}

	public final void setTPKK(final int threshold, final int step) {
		final JSONObject TPKKValues = item.getObj("Trefferpunkte/Körperkraft");
		TPKKValues.put("Schwellenwert", threshold);
		TPKKValues.put("Schadensschritte", step);
		TPKKValues.notifyListeners(null);
	}

	public final void setWM(final int atMod, final int paMod) {
		final JSONObject wm = item.getObj("Waffenmodifikatoren");
		wm.put("Attackemodifikator", atMod);
		wm.put("Parademodifikatior", paMod);
		wm.notifyListeners(null);
	}

	public final ReadOnlyStringProperty specialProperty() {
		return special;
	}

	public final ReadOnlyStringProperty tpkkProperty() {
		return tpkk;
	}

	public final ReadOnlyStringProperty wmProperty() {
		return wm;
	}
}

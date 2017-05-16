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
package dsa41basis.hero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dsa41basis.util.HeroUtil;
import dsa41basis.util.RequirementsUtil;
import dsatool.resources.ResourceManager;
import dsatool.util.Tuple3;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class ProOrCon {

	public enum ChoiceOrTextEnum {
		CHOICE, NONE, TEXT
	}

	protected final JSONObject actual;
	protected final IntegerProperty cost = new SimpleIntegerProperty();
	protected final IntegerProperty numCheaper = new SimpleIntegerProperty();
	protected final StringProperty description;
	protected final ChoiceOrTextEnum first;
	protected final JSONObject hero;
	protected final StringProperty name;
	protected final StringProperty displayName;
	protected final JSONObject proOrCon;
	protected final ChoiceOrTextEnum second;
	protected final IntegerProperty value;
	protected final StringProperty variant;

	protected final int min;
	protected final int max;
	protected final int step;

	private final boolean stepwise;

	public ProOrCon(final String name, final JSONObject hero, final JSONObject proOrCon, final JSONObject actual) {
		this.hero = hero;
		this.proOrCon = proOrCon;
		this.actual = actual;
		this.name = new SimpleStringProperty(name);
		displayName = new SimpleStringProperty(name);

		final boolean hasChoice = proOrCon.containsKey("Auswahl");
		final String text = proOrCon.getString("Freitext");
		stepwise = proOrCon.getBoolOrDefault("Abgestuft", false);
		final JSONObject profession = hero != null ? ResourceManager.getResource("data/Professionen").getObj(hero.getObj("Biografie").getString("Profession"))
				: null;
		final boolean hasBGB = "Breitgefächerte Bildung".equals(name);
		boolean hasBGBVariant = false;
		if (hasBGB) {
			final JSONObject bgbprofession = ResourceManager.getResource("data/Professionen").getObj(actual.getString("Profession"));
			if (bgbprofession != null && bgbprofession.containsKey("Varianten")) {
				hasBGBVariant = true;
			}
		}
		final boolean hasVeteranVariant = "Veteran".equals(name) && profession != null && profession.containsKey("Varianten");

		if (hasBGB) {
			description = new SimpleStringProperty(
					actual.getStringOrDefault("Profession", ResourceManager.getResource("data/Professionen").keySet().iterator().next()));
			first = ChoiceOrTextEnum.CHOICE;
		} else if (hasVeteranVariant) {
			final JSONArray variants = actual.getArrOrDefault("Profession:Modifikation", null);
			description = new SimpleStringProperty(variants != null ? String.join(", ", variants.getStrings()) : "");
			first = ChoiceOrTextEnum.TEXT;
		} else if (hasChoice) {
			final Set<String> choices = getFirstChoiceItems(false);
			description = new SimpleStringProperty(actual.getStringOrDefault("Auswahl", choices.isEmpty() ? "" : choices.iterator().next()));
			first = ChoiceOrTextEnum.CHOICE;
		} else if (text != null) {
			final Set<String> choices = getSecondChoiceItems(false);
			description = new SimpleStringProperty(actual.getStringOrDefault("Freitext", choices.isEmpty() ? text : choices.iterator().next()));
			first = ChoiceOrTextEnum.TEXT;
		} else {
			description = new SimpleStringProperty("");
			first = ChoiceOrTextEnum.NONE;
		}

		if (hasBGBVariant) {
			final JSONArray variants = actual.getArrOrDefault("Profession:Modifikation", null);
			variant = new SimpleStringProperty(variants != null ? String.join(", ", variants.getStrings()) : "");
			second = ChoiceOrTextEnum.TEXT;
		} else if (hasBGB) {
			variant = new SimpleStringProperty("");
			second = ChoiceOrTextEnum.TEXT;
		} else if (hasChoice && text != null) {
			final Set<String> choices = getSecondChoiceItems(false);
			variant = new SimpleStringProperty(actual.getStringOrDefault("Freitext", choices.isEmpty() ? text : choices.iterator().next()));
			second = ChoiceOrTextEnum.TEXT;
		} else {
			variant = new SimpleStringProperty("");
			second = ChoiceOrTextEnum.NONE;
		}

		final Tuple3<Integer, Integer, Integer> bounds = calculateBounds(proOrCon);
		min = bounds._1;
		max = bounds._2;
		step = bounds._3;

		if (stepwise) {
			value = new SimpleIntegerProperty(actual.getIntOrDefault("Stufe", min));
			actual.put("Stufe", value.get());
		} else {
			value = new SimpleIntegerProperty(Integer.MIN_VALUE);
		}

		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));

		if (hero != null && proOrCon.containsKey("Gottheiten")) {
			final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
			final JSONObject liturgyKnowledges = hero.getObj("Talente").getObj("Liturgiekenntnis");
			if (!liturgyKnowledges.keySet().isEmpty()) {
				final String deity = liturgyKnowledges.keySet().iterator().next();
				final JSONObject deities = liturgies.getObj(name).getObjOrDefault("Gottheiten", null);
				if (deities != null && deities.containsKey(deity)) {
					displayName.set(deities.getObj(deity).getStringOrDefault("Name", name));
				}
			}
		}
	}

	private Tuple3<Integer, Integer, Integer> calculateBounds(final JSONObject proOrCon) {
		int step = 1;
		final double cost = proOrCon.getDoubleOrDefault("Kosten", 1.0);
		if (Math.rint(cost) != cost) {
			for (int i = 1; i < 11; ++i) {
				final double quotient = i / cost;
				if (Math.rint(quotient) == quotient) {
					step = (int) quotient;
					break;
				}
			}
		}

		int min = proOrCon.getIntOrDefault("Stufe:Minimum", 1);
		for (int i = 0; i < step; ++i) {
			final double quotient = (min + i) / (double) step;
			if (Math.rint(quotient) == quotient) {
				min = min + i;
				break;
			}
		}

		final int max = proOrCon.getIntOrDefault("Stufe:Maximum", 9999);

		return new Tuple3<>(min, max, step);
	}

	public final IntegerProperty costProperty() {
		return cost;
	}

	public final StringProperty descriptionProperty() {
		return description;
	}

	public final ReadOnlyStringProperty displayNameProperty() {
		return displayName;
	}

	public ChoiceOrTextEnum firstChoiceOrText() {
		return first;
	}

	public JSONObject getActual() {
		return actual;
	}

	public double getBaseCost() {
		double baseCost = 1.0;
		if (proOrCon.containsKey("Kosten")) {
			baseCost = proOrCon.getDouble("Kosten");
		} else if (hero != null && proOrCon.containsKey("Grad")) {
			final JSONObject liturgies = ResourceManager.getResource("data/Liturgien");
			int level = proOrCon.getInt("Grad");
			final JSONObject liturgyKnowledges = hero.getObj("Talente").getObj("Liturgiekenntnis");
			if (!liturgyKnowledges.keySet().isEmpty()) {
				final String deity = liturgyKnowledges.keySet().iterator().next();
				final JSONObject deities = liturgies.getObj(name.get()).getObjOrDefault("Gottheiten", null);
				if (deities != null && deities.containsKey(deity)) {
					level = deities.getObj(deity).getIntOrDefault("Grad", level);
				}
			}
			final JSONObject levels = ResourceManager.getResource("data/Liturgiegrade");
			baseCost = levels.getObj("Grad " + level).getDoubleOrDefault("Kosten", level * 50.0);
		}

		if (proOrCon.containsKey("Kosten:Voraussetzungen")) {
			final JSONArray requirements = proOrCon.getArr("Kosten:Voraussetzungen");
			for (int i = 0; i < requirements.size(); ++i) {
				final JSONObject requirement = requirements.getObj(i);
				if (hero != null && RequirementsUtil.isRequirementFulfilled(hero, requirement, first == ChoiceOrTextEnum.CHOICE ? description.get() : null,
						first == ChoiceOrTextEnum.TEXT ? description.get() : second == ChoiceOrTextEnum.TEXT ? variant.get() : null)) {
					baseCost *= requirement.getDoubleOrDefault("Multiplikativ", 1.0);
					baseCost /= requirement.getDoubleOrDefault("Divisor", 1.0);
				}
			}
		}
		return baseCost;
	}

	public final int getCost() {
		return cost.get();
	}

	public final String getDescription() {
		return description.get();
	}

	public final String getDisplayName() {
		return displayName.get();
	}

	public Set<String> getFirstChoiceItems(final boolean onlyUnused) {
		final Set<String> choices = new LinkedHashSet<>();
		final String choice = "Breitgefächerte Bildung".equals(name.get()) ? "Profession"
				: "Veteran".equals(name.get()) ? "Profession:Variante" : proOrCon.getStringOrDefault("Auswahl", proOrCon.getString("Freitext"));
		if (choice == null) return choices;
		switch (choice) {
		case "Merkmal":
			choices.addAll(ResourceManager.getResource("data/Merkmale").keySet());
			break;
		case "Ritual":
			final JSONObject rituals = ResourceManager.getResource("data/Rituale");
			for (final String group : rituals.keySet()) {
				choices.addAll(rituals.getObj(group).keySet());
			}
			break;
		case "Talentgruppe":
			choices.add("Kampftalente");
			choices.addAll(ResourceManager.getResource("data/Talentgruppen").keySet());
			choices.removeAll(Arrays.asList("Gaben", "Ritualkenntnis", "Liturgiekenntnis"));
			break;
		case "Talent":
			final JSONObject talents = ResourceManager.getResource("data/Talente");
			for (final String talentgroup : talents.keySet()) {
				choices.addAll(talents.getObj(talentgroup).keySet());
			}
			break;
		case "Zauber":
			choices.addAll(ResourceManager.getResource("data/Zauber").keySet());
			break;
		case "Körperliche Eigenschaft":
			final JSONObject attributes = ResourceManager.getResource("data/Eigenschaften");
			for (final String attribute : attributes.keySet()) {
				if (attributes.getObj(attribute).getStringOrDefault("Eigenschaft", "geistig").equals("körperlich")) {
					choices.add(attributes.getObj(attribute).getString("Name"));
				}
			}
			break;
		case "Eigenschaft":
			final JSONObject attributes2 = ResourceManager.getResource("data/Eigenschaften");
			for (final String attribute : attributes2.keySet()) {
				choices.add(attributes2.getObj(attribute).getString("Name"));
			}
			break;
		case "Schlechte Eigenschaft":
			if (hero != null) {
				final JSONObject cons = ResourceManager.getResource("data/Nachteile");
				for (final String con : hero.getObj("Nachteile").keySet()) {
					if (cons.getObj(con).getBoolOrDefault("Schlechte Eigenschaft", false)) {
						choices.add(con);
					}
				}
			}
			break;
		case "Gottheit":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Liturgiekenntnis").keySet());
			break;
		case "Kultur":
			final JSONObject cultures = ResourceManager.getResource("data/Kulturen");
			for (final String cultureName : cultures.keySet()) {
				final JSONObject culture = cultures.getObj(cultureName);
				if (culture.containsKey("Kulturkunde")) {
					choices.add(culture.getString("Kulturkunde"));
				}
				if (culture.containsKey("Varianten")) {
					final JSONObject variants = culture.getObj("Varianten");
					for (final String variantName : variants.keySet()) {
						final JSONObject variant = variants.getObj(variantName);
						if (variant.containsKey("Kulturkunde")) {
							choices.add(variant.getString("Kulturkunde"));
						}
					}
				}
			}
			Collections.addAll(choices, new String[] { "Schwarze Lande", "Trolle", "Grolme" });
			break;
		case "Fernkampftalent":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Fernkampftalente").keySet());
			break;
		case "Kampftalent":
			final JSONObject talents2 = ResourceManager.getResource("data/Talente");
			choices.addAll(talents2.getObj("Nahkampftalente").keySet());
			choices.addAll(talents2.getObj("Fernkampftalente").keySet());
			break;
		case "Profession":
			choices.addAll(ResourceManager.getResource("data/Professionen").keySet());
			break;
		case "Profession:Variante":
			if (hero != null) {
				final JSONObject variants = ResourceManager.getResource("data/Professionen").getObj(hero.getObj("Biografie").getString("Profession"))
						.getObj("Varianten");
				choices.addAll(getVariantStrings(variants));
			}
			break;
		case "Repräsentation":
			final JSONObject representations = ResourceManager.getResource("data/Repraesentationen");
			for (final String representation : representations.keySet()) {
				choices.add(representations.getObj(representation).getString("Name"));
			}
			break;
		case "Ritualkenntnis":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Ritualkenntnis").keySet());
			break;
		case "Kirche":
			choices.addAll(ResourceManager.getResource("data/Talente").getObj("Liturgiekenntnis").keySet());
			choices.add("Bund des wahren Glaubens");
			break;
		case "Waffe":
			final JSONObject weaponItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : weaponItems.keySet()) {
				if (weaponItems.getObj(item).getArr("Waffentypen").contains(description.get())) {
					choices.add(weaponItems.getObj(item).getString("Typ"));
				}
			}
			break;
		case "Rüstung":
			final JSONObject armorItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : armorItems.keySet()) {
				if (armorItems.getObj(item).getArr("Kategorien").contains("Rüstung")) {
					choices.add(armorItems.getObj(item).getString("Typ"));
				}
			}
			break;
		default:
			choices.add(choice);
			break;
		}

		if (onlyUnused && hero != null && second == ChoiceOrTextEnum.NONE) {
			final JSONObject pros = hero.getObj("Vorteile");
			final JSONObject cons = hero.getObj("Nachteile");
			final JSONObject skills = hero.getObj("Sonderfertigkeiten");

			JSONArray used = null;
			if (pros.containsKey(getName())) {
				used = pros.getArr(getName());
			} else if (cons.containsKey(getName())) {
				used = cons.getArr(getName());
			} else if (skills.containsKey(getName())) {
				used = skills.getArr(getName());
			}

			if (used != null) {
				for (int i = 0; i < used.size(); ++i) {
					final JSONObject current = used.getObj(i);
					if (first == ChoiceOrTextEnum.CHOICE) {
						choices.remove(current.getString("Auswahl"));
					} else {
						choices.remove(current.getString("Freitext"));
					}
				}
			}
		}

		if (choices.isEmpty()) {
			choices.add("");
		}

		return choices;
	}

	public int getMaxValue() {
		return max;
	}

	public int getMinValue() {
		return min;
	}

	public final String getName() {
		return name.get();
	}

	public final int getNumCheaper() {
		return numCheaper.get();
	}

	public JSONObject getProOrCon() {
		return proOrCon;
	}

	public Set<String> getSecondChoiceItems(final boolean onlyUnused) {
		final Set<String> choices = new LinkedHashSet<>();
		final String choice = "Breitgefächerte Bildung".equals(name.get()) ? "Profession:Variante" : proOrCon.getString("Freitext");
		if (choice == null) return choices;
		switch (choice) {
		case "Spezialisierung":
			if ("Talent".equals(proOrCon.getString("Auswahl"))) {
				final JSONObject talent = HeroUtil.findTalent(description.get())._1;
				if (talent != null && talent.containsKey("Spezialisierungen")) {
					final JSONArray specializations = talent.getArr("Spezialisierungen");
					for (int i = 0; i < specializations.size(); ++i) {
						choices.add(specializations.getString(i));
					}
				}
			} else if ("Zauber".equals(proOrCon.getString("Auswahl"))) {
				final JSONObject spell = ResourceManager.getResource("data/Zauber").getObj(description.get());
				if (spell.containsKey("Spontane Modifikationen")) {
					final JSONArray spoMos = spell.getArr("Spontane Modifikationen");
					for (int i = 0; i < spoMos.size(); ++i) {
						choices.add(spoMos.getString(i));
					}
				}
				if (spell.containsKey("Varianten")) {
					final JSONArray variants = spell.getArr("Varianten");
					for (int i = 0; i < variants.size(); ++i) {
						choices.add(variants.getString(i));
					}
				}
			}
			break;
		case "Waffe":
			final JSONObject weaponItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : weaponItems.keySet()) {
				if (weaponItems.getObj(item).getArr("Waffentypen").contains(description.get())) {
					choices.add(weaponItems.getObj(item).getString("Typ"));
				}
			}
			break;
		case "Rüstung":
			final JSONObject armorItems = ResourceManager.getResource("data/Ausruestung");
			for (final String item : armorItems.keySet()) {
				if (armorItems.getObj(item).getArr("Kategorien").contains("Rüstung")) {
					choices.add(armorItems.getObj(item).getString("Typ"));
				}
			}
			break;
		case "Profession:Variante":
			final JSONObject variants = ResourceManager.getResource("data/Professionen").getObj(description.get()).getObj("Varianten");
			choices.addAll(getVariantStrings(variants));
			break;
		default:
			choices.add(choice);
			break;
		}

		if (onlyUnused && hero != null) {
			final JSONObject pros = hero.getObj("Vorteile");
			final JSONObject cons = hero.getObj("Nachteile");
			final JSONObject skills = hero.getObj("Sonderfertigkeiten");

			JSONArray used = null;
			if (pros.containsKey(getName())) {
				used = pros.getArr(getName());
			} else if (cons.containsKey(getName())) {
				used = cons.getArr(getName());
			} else if (skills.containsKey(getName())) {
				used = skills.getArr(getName());
			}

			if (used != null) {
				for (int i = 0; i < used.size(); ++i) {
					final JSONObject current = used.getObj(i);
					if (!getDescription().equals(current.getString("Auswahl"))) {
						continue;
					}
					choices.remove(current.getString("Freitext"));
				}
			}
		}

		if (choices.isEmpty()) {
			choices.add("");
		}

		return choices;
	}

	public int getStep() {
		return step;
	}

	public final int getValue() {
		return value.get();
	}

	public final String getVariant() {
		return variant.get();
	}

	private List<String> getVariantStrings(final JSONObject variants) {
		final List<String> result = new ArrayList<>();
		final List<String> combinable = new ArrayList<>();
		for (final String variantName : variants.keySet()) {
			final JSONObject variant = variants.getObj(variantName);
			if (variant.getBoolOrDefault("kombinierbar", false)) {
				final List<String> newCombinations = new ArrayList<>(combinable.size());
				for (final String variantString : combinable) {
					newCombinations.add(variantString + ", " + variantName);
				}
				combinable.add(variantName);
				combinable.addAll(newCombinations);
			} else {
				final List<String> newVariants = getVariantStrings(variant.getObj("Varianten"));
				for (final String variantString : newVariants) {
					if (!variantString.equals(variantName) && !variantString.startsWith(variantName + ",")) {
						result.add(variantName + ", " + variantString);
					} else {
						result.add(variantString);
					}
				}
				if (newVariants.isEmpty()) {
					result.add(variantName);
				}
			}
		}
		final List<String> newCombinations = new ArrayList<>(combinable.size());
		for (final String variantString : result) {
			for (final String combination : combinable) {
				newCombinations.add(variantString + ", " + combination);
			}
		}
		result.addAll(newCombinations);
		return result;
	}

	public final ReadOnlyStringProperty nameProperty() {
		return name;
	}

	public final IntegerProperty numCheaperProperty() {
		return numCheaper;
	}

	public ChoiceOrTextEnum secondChoiceOrText() {
		return second;
	}

	public final void setCost(final int cost) {
		actual.put("Kosten", cost);
		this.cost.set(cost);
		actual.notifyListeners(null);
	}

	public void setDescription(final String description) {
		HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		if (proOrCon.containsKey("Auswahl")) {
			actual.put("Auswahl", description);
		} else if (proOrCon.containsKey("Freitext")) {
			actual.put("Freitext", description);
		} else if ("Breitgefächerte Bildung".equals(name.get())) {
			actual.put("Profession", description);
			variant.set("");
		} else if ("Veteran".equals(name.get())) {
			final JSONArray variants = new JSONArray(actual);
			final String[] variantStrings = description.trim().split(", ");
			for (final String variantName : variantStrings) {
				variants.add(variantName);
			}
			actual.put("Profession:Modifikation", variants);
		}
		HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		this.description.set(description);
		actual.notifyListeners(null);
	}

	public void setNumCheaper(final int numCheaper) {
		if (numCheaper > 1) {
			actual.put("Verbilligungen", numCheaper);
		} else {
			actual.removeKey("Verbilligungen");
		}
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		this.numCheaper.set(numCheaper);
		actual.notifyListeners(null);
	}

	public void setValue(final int value) {
		HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		actual.put("Stufe", value);
		HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		updateCost(value, actual.getString("Auswahl"), actual.getString("Freitext"));
		this.value.set(value);
		actual.notifyListeners(null);
	}

	public void setVariant(final String variant) {
		HeroUtil.unapplyEffect(hero, name.get(), proOrCon, actual);
		if (proOrCon.containsKey("Auswahl") && proOrCon.containsKey("Freitext")) {
			actual.put("Freitext", variant);
		} else if ("Breitgefächerte Bildung".equals(name.get())) {
			final JSONArray variants = new JSONArray(actual);
			final String[] variantStrings = variant.trim().split(", ");
			for (final String variantName : variantStrings) {
				variants.add(variantName);
			}
			actual.put("Profession:Modifikation", variants);
		}
		HeroUtil.applyEffect(hero, name.get(), proOrCon, actual);
		updateCost(value.get(), actual.getString("Auswahl"), actual.getString("Freitext"));
		this.variant.set(variant);
		actual.notifyListeners(null);
	}

	protected void updateCost(final int value, final String choice, final String text) {
		cost.setValue((int) (getBaseCost() * (stepwise ? value : 1)));
		if (actual.containsKey("Kosten")) {
			cost.set(actual.getDouble("Kosten").intValue());
		} else if (hero != null) {
			final JSONObject cheaperSkills = hero.getObj("Verbilligte Sonderfertigkeiten");
			if (cheaperSkills.containsKey(name.get())) {
				if (proOrCon.containsKey("Auswahl") || proOrCon.containsKey("Freitext")) {
					final JSONArray cheaperSkill = cheaperSkills.getArr(name.get());
					for (int i = 0; i < cheaperSkill.size(); ++i) {
						final JSONObject variant = cheaperSkill.getObj(i);
						if (proOrCon.containsKey("Auswahl") && !variant.getStringOrDefault("Auswahl", "").equals(choice)) {
							continue;
						}
						if (proOrCon.containsKey("Freitext") && !variant.getStringOrDefault("Freitext", "").equals(text)) {
							continue;
						}
						numCheaper.set(variant.getIntOrDefault("Verbilligungen", 1));
						cost.set((int) Math.round(cost.get() / Math.pow(2, numCheaper.get())));
					}
				} else {
					final JSONObject cheaperSkill = cheaperSkills.getObj(name.get());
					numCheaper.set(cheaperSkill.getIntOrDefault("Verbilligungen", 1));
					cost.set((int) Math.round(cost.get() / Math.pow(2, numCheaper.get())));
				}
			} else if (hero.getObj("Nachteile").containsKey("Elfische Weltsicht")) {
				final JSONObject pros = ResourceManager.getResource("data/Vorteile");
				final JSONObject cons = ResourceManager.getResource("data/Nachteile");
				if (!proOrCon.getBoolOrDefault("Leitsonderfertigkeit", false) && !pros.containsKey(name.get()) && !cons.containsKey(name.get())) {
					cost.set(cost.get() + (cost.get() + 1) / 2);
				}
			}
		}
	}

	public final IntegerProperty valueProperty() {
		return value;
	}

	public final StringProperty variantProperty() {
		return variant;
	}
}

package com.laytonsmith.core.events.drivers;

import com.laytonsmith.PureUtilities.StringUtils;
import com.laytonsmith.abstraction.MCInventory;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.abstraction.enums.MCSlotType;
import com.laytonsmith.abstraction.events.MCInventoryClickEvent;
import com.laytonsmith.abstraction.events.MCInventoryCloseEvent;
import com.laytonsmith.abstraction.events.MCInventoryOpenEvent;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.CHLog.Tags;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.LogLevel;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.*;
import com.laytonsmith.core.events.AbstractEvent;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.Prefilters;
import com.laytonsmith.core.events.Prefilters.PrefilterType;
import com.laytonsmith.core.exceptions.EventException;
import com.laytonsmith.core.exceptions.PrefilterNonMatchException;
import com.laytonsmith.core.natives.interfaces.Mixed;
import java.util.Map;

/**
 *
 * @author jb_aero
 */
public class InventoryEvents {
	public static String docs() {
		return "Contains events related to inventory.";
	}

	@api
	public static class inventory_click extends AbstractEvent {

		public String getName() {
			return "inventory_click";
		}

		public String docs() {
			return "{slottype: <macro> The type of slot being clicked, can be "
					+ StringUtils.Join(MCSlotType.values(), ", ", ", or ")
					+ " | slotitem: <item match> } "
					+ "Fired when a player clicks a slot in any inventory. "
					+ "{player: The player who clicked | " /*"{player: The player who clicked | viewers: everyone looking in this inventory | "*/
					+ "rightclick: true/false if this was a right click | shiftclick: true/false if shift was being held | "
					+ "slot: the number of the slot | rawslot: the number of the slot in whole inventory window | slottype |"
					+ " slotitem | inventorytype | inventorysize: number of slots in opened inventory | cursoritem} "
					+ "{slotitem: the item currently in the clicked slot | cursoritem: the item on the cursor} "
					+ "{} ";
		}

		public boolean matches(Map<String, Mixed> prefilter, BindableEvent event, Target t)
				throws PrefilterNonMatchException {
			if (event instanceof MCInventoryClickEvent) {
				MCInventoryClickEvent e = (MCInventoryClickEvent) event;

				Prefilters.match(prefilter, "slottype", e.getSlotType().name(), PrefilterType.MACRO);
				Prefilters.match(prefilter, "slotitem", Static.ParseItemNotation(e.getCurrentItem()), PrefilterType.ITEM_MATCH);

				return true;
			}
			return false;
		}

		public BindableEvent convert(CArray manualObject) {
			return null;
		}

		public Map<String, Mixed> evaluate(BindableEvent event)
				throws EventException {
			if (event instanceof MCInventoryClickEvent) {
				MCInventoryClickEvent e = (MCInventoryClickEvent) event;
				Map<String, Mixed> map = evaluate_helper(event);

				map.put("player", new CString(e.getWhoClicked().getName(), Target.UNKNOWN));
//				CArray viewers = new CArray(Target.UNKNOWN);
//				for (MCHumanEntity viewer : e.getViewers()) {
//					viewers.push(new CString(viewer.getName(), Target.UNKNOWN));
//				}
//				map.put("viewers", viewers);

				map.put("rightclick", new CBoolean(e.isRightClick(), Target.UNKNOWN));
				map.put("shiftclick", new CBoolean(e.isShiftClick(), Target.UNKNOWN));
				map.put("cursoritem", ObjectGenerator.GetGenerator().item(e.getCursor(), Target.UNKNOWN));

				map.put("slot", new CInt(e.getSlot(), Target.UNKNOWN));
				map.put("rawslot", new CInt(e.getRawSlot(), Target.UNKNOWN));
				map.put("slottype", new CString(e.getSlotType().name(), Target.UNKNOWN));
				map.put("slotitem", ObjectGenerator.GetGenerator().item(e.getCurrentItem(), Target.UNKNOWN));
				
				CArray items = CArray.GetAssociativeArray(Target.UNKNOWN);
				MCInventory inv = e.getInventory();
				for (int i = 0; i < inv.getSize(); i++) {
					Construct c = ObjectGenerator.GetGenerator().item(inv.getItem(i), Target.UNKNOWN);
					items.set(i, c, Target.UNKNOWN);
				}
				map.put("inventory", items);
				map.put("inventorytype", new CString(inv.getType().name(), Target.UNKNOWN));
				map.put("inventorysize", new CInt(inv.getSize(), Target.UNKNOWN));

				return map;
			} else {
				throw new EventException("Cannot convert e to MCInventoryClickEvent");
			}
		}

		public Driver driver() {
			return Driver.INVENTORY_CLICK;
		}

		public boolean modifyEvent(String key, Mixed value,
				BindableEvent event, Target t) {
			if (event instanceof MCInventoryClickEvent) {
				MCInventoryClickEvent e = (MCInventoryClickEvent) event;

				if (key.equalsIgnoreCase("slotitem")) {
					e.setCurrentItem(ObjectGenerator.GetGenerator().item((Construct)value, Target.UNKNOWN));
					return true;
				}
				if (key.equalsIgnoreCase("cursoritem")) {
					e.setCursor(ObjectGenerator.GetGenerator().item((Construct)value, Target.UNKNOWN));
					return true;
				}
			}
			return false;
		}

		@Override
		public void cancel(BindableEvent o, boolean state) {
			MCInventoryClickEvent ic = ((MCInventoryClickEvent)o);
            ic.setCancelled(state);
			StaticLayer.GetServer().getPlayer(ic.getWhoClicked().getName()).updateInventory();
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}

	}

	@api
	public static class inventory_open extends AbstractEvent {

		public String getName() {
			return "inventory_open";
		}

		public String docs() {
			return "{} "
					+ "Fired when a player opens an inventory. "
					+ "{player: The player | " /*"{player: The player who clicked | viewers: everyone looking in this inventory | "*/
					+ "inventory: the inventory items in this inventory | "
					+ "inventorytype: type of inventory} "
					+ "{} "
					+ "{} ";
		}

		public boolean matches(Map<String, Mixed> prefilter, BindableEvent event, Target t)
				throws PrefilterNonMatchException {
			return true;
		}

		public BindableEvent convert(CArray manualObject) {
			return null;
		}

		public Map<String, Mixed> evaluate(BindableEvent event)
				throws EventException {
			if (event instanceof MCInventoryOpenEvent) {
				MCInventoryOpenEvent e = (MCInventoryOpenEvent) event;
				Map<String, Mixed> map = evaluate_helper(event);

				map.put("player", new CString(e.getPlayer().getName(), Target.UNKNOWN));

				CArray items = CArray.GetAssociativeArray(Target.UNKNOWN);
				MCInventory inv = e.getInventory();

				for (int i = 0; i < inv.getSize(); i++) {
					Construct c = ObjectGenerator.GetGenerator().item(inv.getItem(i), Target.UNKNOWN);
					items.set(i, c, Target.UNKNOWN);
				}

				map.put("inventory", items);

				map.put("inventorytype", new CString(e.getInventory().getType().name(), Target.UNKNOWN));

				return map;
			} else {
				throw new EventException("Cannot convert e to MCInventoryOpenEvent");
			}
		}

		public Driver driver() {
			return Driver.INVENTORY_OPEN;
		}

		public boolean modifyEvent(String key, Mixed value,
				BindableEvent event, Target t) {
			return false;
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}

	}

	@api
	public static class inventory_close extends AbstractEvent {

		public String getName() {
			return "inventory_close";
		}

		public String docs() {
			return "{} "
					+ "Fired when a player closes an inventory. "
					+ "{player: The player | " /*"{player: The player who clicked | viewers: everyone looking in this inventory | "*/
					+ "inventory: the inventory items in this inventory | "
					+ "inventorytype: type of inventory} "
					+ "{} "
					+ "{} ";
		}

		public boolean matches(Map<String, Mixed> prefilter, BindableEvent event, Target t)
				throws PrefilterNonMatchException {
			return true;
		}

		public BindableEvent convert(CArray manualObject) {
			return null;
		}

		public Map<String, Mixed> evaluate(BindableEvent event)
				throws EventException {
			if (event instanceof MCInventoryCloseEvent) {
				MCInventoryCloseEvent e = (MCInventoryCloseEvent) event;
				Map<String, Mixed> map = evaluate_helper(event);

				map.put("player", new CString(e.getPlayer().getName(), Target.UNKNOWN));

				CArray items = CArray.GetAssociativeArray(Target.UNKNOWN);
				MCInventory inv = e.getInventory();

				for (int i = 0; i < inv.getSize(); i++) {
					Construct c = ObjectGenerator.GetGenerator().item(inv.getItem(i), Target.UNKNOWN);
					items.set(i, c, Target.UNKNOWN);
				}

				map.put("inventory", items);

				map.put("inventorytype", new CString(e.getInventory().getType().name(), Target.UNKNOWN));

				return map;
			} else {
				throw new EventException("Cannot convert e to MCInventoryCloseEvent");
			}
		}

		public Driver driver() {
			return Driver.INVENTORY_CLOSE;
		}

		public boolean modifyEvent(String key, Mixed value,
				BindableEvent event, Target t) {
			return false;
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}

	}
}

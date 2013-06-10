package com.laytonsmith.core.functions;

import com.laytonsmith.PureUtilities.StringUtils;
import com.laytonsmith.abstraction.*;
import com.laytonsmith.abstraction.blocks.MCBlockFace;
import com.laytonsmith.abstraction.enums.MCEntityEffect;
import com.laytonsmith.abstraction.enums.MCEntityType;
import com.laytonsmith.abstraction.enums.MCEquipmentSlot;
import com.laytonsmith.abstraction.enums.MCProjectileType;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.LogLevel;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Optimizable;
import com.laytonsmith.core.ParseTree;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.arguments.ArgList;
import com.laytonsmith.core.arguments.Argument;
import com.laytonsmith.core.arguments.ArgumentBuilder;
import com.laytonsmith.core.arguments.Generic;
import com.laytonsmith.core.arguments.Signature;
import com.laytonsmith.core.constructs.*;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import com.laytonsmith.core.natives.MEquipment;
import com.laytonsmith.core.natives.MLocation;
import com.laytonsmith.core.natives.MPotion;
import com.laytonsmith.core.natives.annotations.FormatString;
import com.laytonsmith.core.natives.annotations.NonNull;
import com.laytonsmith.core.natives.annotations.Ranged;
import com.laytonsmith.core.natives.interfaces.Mixed;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jb_aero
 */
public class EntityManagement {
	public static String docs(){
        return "This class of functions allow entities to be managed.";
    }

	public static abstract class EntityFunction extends AbstractFunction {
		public boolean isRestricted() {
			return true;
		}

		public Boolean runAsync() {
			return false;
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}
	}

	public static abstract class EntityGetterFunction extends EntityFunction {
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.BadEntityException};
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}
	}

	public static abstract class EntitySetterFunction extends EntityFunction {
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.FormatException, ExceptionType.CastException,
					ExceptionType.BadEntityException};
		}

		public Integer[] numArgs() {
			return new Integer[]{2};
		}
	}

	@api
	public static class all_entities extends EntityFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.InvalidWorldException, ExceptionType.FormatException,
					ExceptionType.CastException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			CArray ret = new CArray(t);
			if (args.length == 0) {
				for (MCWorld w : Static.getServer().getWorlds()) {
					for (MCEntity e : w.getEntities()) {
						ret.push(new CInt(e.getEntityId(), t));
					}
				}
			} else {
				MCWorld w;
				MCChunk c;
				if (args.length == 3) {
					w = Static.getServer().getWorld(args[0].val());
					try {
						int x = args[1].primitive(t).castToInt32(t);
						int z = args[2].primitive(t).castToInt32(t);
						c = w.getChunkAt(x, z);
					} catch (ConfigRuntimeException cre) {
						CArray l = CArray.GetAssociativeArray(t);
						l.set("x", args[1], t);
						l.set("z", args[2], t);
						c = w.getChunkAt(ObjectGenerator.GetGenerator().location(l, w, t));
					}
					for (MCEntity e : c.getEntities()) {
						ret.push(new CInt(e.getEntityId(), t));
					}
				} else {
					if (args[0] instanceof CArray) {
						c = ObjectGenerator.GetGenerator().location(args[0], null, t).getChunk();
						for (MCEntity e : c.getEntities()) {
							ret.push(new CInt(e.getEntityId(), t));
						}
					} else {
						w = Static.getServer().getWorld(args[0].val());
						for (MCEntity e : w.getEntities()) {
							ret.push(new CInt(e.getEntityId(), t));
						}
					}
				}
			}
			return ret;
		}

		public String getName() {
			return "all_entities";
		}

		public Integer[] numArgs() {
			return new Integer[]{0, 1, 3};
		}

		public String docs() {
			return "Returns an array of IDs for all entities in the given"
					+ " scope. With no args, this will return all entities loaded on the entire server. If the first"
					+ " argument is given and is a location, only entities in the chunk containin that location will"
					+ " be returned, or if it is a world only entities in that world will be returned. If all 3"
					+ " arguments are given, only entities in the chunk with those coords will be returned. This can"
					+ " take chunk coords (ints) or location coords (doubles)."; 
		}
		
		public Argument returnType() {
			return new Argument("The entity ids of the entities in the given scope", CArray.class).setGenerics(new Generic(CInt.class));
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Signature(1, 
							new Argument("", CString.class, "world").setOptionalDefaultNull()
						), new Signature(2, 
							new Argument("", CString.class, "world"),
							new Argument("", CNumber.class, "x"),
							new Argument("", CNumber.class, "z")
						), new Signature(3, 
							new Argument("A location array, from with the chunk will be determined", MLocation.class, "locationArray")
						)
					);
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
					new ExampleScript("Getting all entities in a world", "msg(all_entities(pworld()))",
							"Sends you an array of all entities in your world."),
					new ExampleScript("Getting entities in a chunk", "msg(all_entities(pworld(), 5, -3))",
							"Sends you an array of all entities in chunk (5,-3)."),
					new ExampleScript("Getting entities in your chunk", "msg(all_entities(ploc()))",
							"Sends you an array of all entities in the chunk you are in.")
			};
		}

	}

	@api
	public static class entity_loc extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity e = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			return ObjectGenerator.GetGenerator().location(e.getLocation());
		}

		public String getName() {
			return "entity_loc";
		}

		public String docs() {
			return "Returns the location array for this entity, if it exists."
					+ " This array will be compatible with any function that expects a location.";
		}
		
		public Argument returnType() {
			return new Argument("The location of this entity", MLocation.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity id to check", CInt.class, "entityID")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
					new ExampleScript("Sample output", "entity_loc(5048)",
							"{0: -3451.96, 1: 65.0, 2: 718.521, 3: world, 4: -170.9, 5: 35.5, pitch: 35.5,"
							+ " world: world, x: -3451.96, y: 65.0, yaw: -170.9, z: 718.521}")
			};
		}

	}

	@api
	public static class set_entity_loc extends EntitySetterFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.BadEntityException, ExceptionType.FormatException,
					ExceptionType.CastException, ExceptionType.InvalidWorldException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity e = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			MCLocation l;
			if (args[1] instanceof CArray) {
				l = ObjectGenerator.GetGenerator().location((CArray) args[1], e.getWorld(), t);
			} else {
				throw new Exceptions.FormatException("An array was expected but recieved " + args[1], t);
			}
			return new CBoolean(e.teleport(l), t);
		}

		public String getName() {
			return "set_entity_loc";
		}

		public String docs() {
			return "boolean {entityID, locationArray} Teleports the entity to the given location and returns whether"
					+ " the action was successful. Note this can set both location and direction.";
		}
		
		public Argument returnType() {
			return new Argument("True if the action was successful", CBoolean.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", MLocation.class, "locationArray")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Teleporting an entity to you", "set_entity_loc(386, ploc())",
						"The entity will teleport to the block you are standing on."),
				new ExampleScript("Teleporting an entity to another", "set_entity_loc(201, entity_location(10653))",
						"The entity will teleport to the other and face the same direction, if they both exist."),
				new ExampleScript("Setting location with a normal array",
						"set_entity_loc(465, array(214, 64, 1812, 'world', -170, 10))", "This set location and direction."),
				new ExampleScript("Setting location with an associative array",
						"set_entity_loc(852, array(x: 214, y: 64, z: 1812, world: 'world', yaw: -170, pitch: 10))",
						"This also sets location and direction")
			};
		}

	}

	@api
	public static class entity_velocity extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			
			MCEntity e = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			CArray va = ObjectGenerator.GetGenerator().velocity(e.getVelocity(), t);
			return va;
		}

		public String getName() {
			return "entity_velocity";
		}

		public String docs() {
			return "Returns an associative array indicating the x/y/z components of this entity's velocity."
					+ " As a convenience, the magnitude is also included.";
		}
		
		public Argument returnType() {
			return new Argument("", CArray.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
					new ExampleScript("A stationary entity", "msg(entity_velocity(235))",
							"{magnitude: 0.0, x: 0.0, y: 0.0, z: 0.0}")
			};
		}

	}

	@api
	public static class set_entity_velocity extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			
			MCEntity e = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			e.setVelocity(ObjectGenerator.GetGenerator().velocity(args[1], t));
			return new CVoid(t);
		}

		public String getName() {
			return "set_entity_velocity";
		}

		public String docs() {
			return "Sets the velocity of this entity according to the supplied xyz array. All 3"
					+ " values default to 0, so an empty array will simply stop the entity's motion. Both normal and"
					+ " associative arrays are accepted.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("An array that represents the vector velocity, with the keys x, y, and z set", CArray.class, "vector")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
					new ExampleScript("Setting a bounce with a normal array", "set_entity_velocity(235, array(0, 0.5, 0))",
							"The entity just hopped, unless it was an item frame or painting."),
					new ExampleScript("Setting a bounce with an associative array", "set_entity_velocity(235, array(y: 0.5))",
							"The entity just hopped, unless it was an item frame or painting.")
			};
		}

	}

	@api
	public static class entity_remove extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity ent = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			if (ent == null) {
				return new CVoid(t);
			} else if (ent instanceof MCHumanEntity) {
				throw new ConfigRuntimeException("Cannot remove human entity (" + ent.getEntityId() + ")!",
						ExceptionType.BadEntityException, t);
			} else {
				ent.remove();
				return new CVoid(t);
			}
		}

		public String getName() {
			return "entity_remove";
		}

		public String docs() {
			return "Removes the specified entity from the world, without any drops or animations. "
				+ "Note: you can't remove players. As a safety measure for working with NPC plugins, it will "
				+ "not work on anything human, even if it is not a player.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}

	}

	@api
	public static class entity_type extends EntityGetterFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity ent;
			int id = args[0].primitive(t).castToInt32(t);
			try {
				ent = Static.getEntity(id, t);
			} catch (ConfigRuntimeException cre) {
				return Construct.GetNullConstruct(CString.class, t);
			}
			return new CString(ent.getType().name(), t);
		}

		public String getName() {
			return "entity_type";
		}

		public String docs() {
			return "Returns the EntityType of the entity with the specified ID.";
		}
		
		public Argument returnType() {
			return new Argument("The type of the entity", CString.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}
	}

	@api
	public static class get_entity_age extends EntityGetterFunction {
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			MCEntity ent = Static.getEntity(list.getInt("entityID", t), t);
			return new CInt(ent.getTicksLived(), t);
		}

		public String getName() {
			return "get_entity_age";
		}

		public String docs() {
			return "Returns the entity age as an integer, represented by server ticks.";
		}
		
		public Argument returnType() {
			return new Argument("The age of the entity, in server ticks", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID").addAnnotation(new NonNull())
					);
		}
	}

	@api
	public static class set_entity_age extends EntitySetterFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.BadEntityException,
					ExceptionType.RangeException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			MCEntity ent = Static.getEntity(list.getInt("entityID", t), t);
			int age = list.getInt("age", t);
			ent.setTicksLived(age);
			return new CVoid(t);
		}

		public String getName() {
			return "set_entity_age";
		}

		public String docs() {
			return "Sets the age of the entity to the specified int, represented by server ticks.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("The age of the entity, in server ticks", CInt.class, "age").addAnnotation(Ranged.POSITIVE).addAnnotation(new NonNull())
					);
		}
	}

	@api
	public static class get_mob_age extends EntityGetterFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.UnageableMobException, ExceptionType.CastException,
					ExceptionType.BadEntityException};
		}

		public CInt exec(Target t, Environment environment,
				Mixed... args) throws ConfigRuntimeException {
			int id = args[0].primitive(t).castToInt32(t);
			MCEntity ent = Static.getLivingEntity(id, t);
			if (ent == null) {
				return Construct.GetNullConstruct(CInt.class, t);
			} else if (ent instanceof MCAgeable) {
				MCAgeable mob = ((MCAgeable) ent);
				return new CInt(mob.getAge(), t);
			} else {
				throw new ConfigRuntimeException("The specified entity does not age", ExceptionType.UnageableMobException, t);
			}
		}

		public String getName() {
			return "get_mob_age";
		}

		public String docs() {
			return "int {entityID} Returns the mob's age as an integer. Zero represents the point of adulthood. Throws an"
					+ " UnageableMobException if the mob is not a type that ages";
		}
		
		public Argument returnType() {
			return new Argument("The mob's age", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}
	}

	@api
	public static class set_mob_age extends EntityFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.UnageableMobException, ExceptionType.CastException,
					ExceptionType.BadEntityException};
		}

		public Construct exec(Target t, Environment environment,
				Mixed... args) throws ConfigRuntimeException {
			int id = args[0].primitive(t).castToInt32(t);
			int age = args[1].primitive(t).castToInt32(t);
			boolean lock = false;
			if (args.length == 3) {
				lock = args[2].primitive(t).castToBoolean();
			}
			MCLivingEntity ent = Static.getLivingEntity(id, t);
			if (ent == null) {
				throw new ConfigRuntimeException("No mob was found with id " + id, ExceptionType.BadEntityException, t);
			} else if (ent instanceof MCAgeable) {
				MCAgeable mob = ((MCAgeable) ent);
				mob.setAge(age);
				mob.setAgeLock(lock);
				return new CVoid(t);
			} else {
				throw new ConfigRuntimeException("The specified entity does not age", ExceptionType.UnageableMobException, t);
			}
		}

		public String getName() {
			return "set_mob_age";
		}

		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		public String docs() {
			return "sets the age of the mob to the specified int, and locks it at that age"
					+ " if lockAge is true, but by default it will not. Throws a UnageableMobException if the mob does not age naturally.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("The age to set", CInt.class, "age"),
						new Argument("If true, locks this age in", CBoolean.class, "lock").setOptionalDefault(false)
					);
		}
	}

	@api
	public static class get_mob_effects extends EntityGetterFunction {

		public Construct exec(Target t, Environment environment,
				Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity mob = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			return ObjectGenerator.GetGenerator().potions(mob.getEffects(), t);
		}

		public String getName() {
			return "get_mob_effects";
		}

		public String docs() {
			return "Returns an array of potion arrays showing"
					+ " the effects on this mob.";
		}
		
		public Argument returnType() {
			return new Argument("", CArray.class).setGenerics(new Generic(MPotion.class));
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity ID of the mob to check.", CInt.class, "entityId")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{new ExampleScript("Basic use", "msg(get_mob_effects(259))",
					"{{ambient: false, id: 1, seconds: 30, strength: 1}}")};
		}

	}

	@api
	public static class set_mob_effect extends EntityFunction {

		public String getName() {
			return "set_mob_effect";
		}

		public Integer[] numArgs() {
			return new Integer[]{3, 4, 5};
		}

		public String docs() {
			return "Not all potions work of course, but effect is 1-19. Seconds defaults to 30."
					+ " If the potionID is out of range, a RangeException is thrown, because out of range potion effects"
					+ " cause the client to crash, fairly hardcore. See http://www.minecraftwiki.net/wiki/Potion_effects for a"
					+ " complete list of potions that can be added. To remove an effect, set the strength (or duration) to 0."
					+ " Strength is the number of levels to add to the base power (effect level 1). Ambient takes a boolean"
					+ " of whether the particles should be less noticeable. The function returns true if the effect was"
					+ " added or removed as desired, and false if it wasn't (however, this currently only will happen if an effect is attempted"
					+ " to be removed, yet isn't already on the mob).";
		}
		
		public Argument returnType() {
			return new Argument("True if the effect was added or removed as desired", CBoolean.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", CInt.class, "potionID").addAnnotation(new Ranged(1, 20)),
						new Argument("The strength of the effect", CInt.class, "strength"),
						new Argument("The number of seconds to apply the effect", CInt.class, "seconds").setOptionalDefault(30)
					);
		}

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.FormatException,
					ExceptionType.BadEntityException, ExceptionType.RangeException};
		}

		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			int id = args[0].primitive(t).castToInt32(t);
			MCLivingEntity mob = Static.getLivingEntity(id, t);
			int effect = args[1].primitive(t).castToInt32(t);
			int strength = args[2].primitive(t).castToInt32(t);
			int seconds = 30;
			boolean ambient = false;
			if (args.length >= 4) {
				seconds = args[3].primitive(t).castToInt32(t);
			}
			if (args.length == 5) {
				ambient = args[4].primitive(t).castToBoolean();
			}

			if (seconds == 0) {
				return new CBoolean(mob.removeEffect(effect), t);
			} else {
				mob.addEffect(effect, strength, seconds, ambient, t);
				return new CBoolean(true, t);
			}
		}
	}

	@api(environments={CommandHelperEnvironment.class})
	public static class shoot_projectile extends EntityFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.BadEntityException, ExceptionType.FormatException,
					ExceptionType.PlayerOfflineException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCCommandSender cmdsr = environment.getEnv(CommandHelperEnvironment.class).GetCommandSender();
			MCLivingEntity ent = null;
			int id;
			MCProjectileType toShoot = MCProjectileType.FIREBALL;
			if (args.length >= 1) {
				try {
					id = Static.GetPlayer(args[0], t).getEntityId();
				} catch (ConfigRuntimeException notPlayer) {
					try {
						id = args[0].primitive(t).castToInt32(t);
					} catch (ConfigRuntimeException notEntIDEither) {
						throw new ConfigRuntimeException("Could not find an entity matching " + args[0] + "!",
								ExceptionType.BadEntityException, t);
					}
				}
			} else {
				if (cmdsr instanceof MCPlayer) {
					id = ((MCLivingEntity) cmdsr).getEntityId();
					Static.AssertPlayerNonNull((MCPlayer) cmdsr, t);
				} else {
					throw new ConfigRuntimeException("A player was expected!", ExceptionType.PlayerOfflineException, t);
				}
			}
			if (args.length == 2) {
				try {
					toShoot = MCProjectileType.valueOf(args[1].toString().toUpperCase());
				} catch (IllegalArgumentException badEnum) {
					throw new ConfigRuntimeException(args[1] + " is not a valid Projectile", ExceptionType.FormatException, t);
				}
			}
			ent = Static.getLivingEntity(id, t);
			return new CInt(ent.launchProjectile(toShoot).getEntityId(), t);
		}

		public String getName() {
			return "shoot_projectile";
		}

		public Integer[] numArgs() {
			return new Integer[]{0, 1, 2};
		}

		public String docs() {
			return "int {[player[, projectile]] | [entityID[, projectile]]} shoots a projectile from the entity or player"
					+ " specified, or the current player if no arguments are passed. If no projectile is specified,"
					+ " it defaults to a fireball. Returns the EntityID of the projectile. Valid projectiles: "
					+ StringUtils.Join(MCProjectileType.values(), ", ", ", or ", " or ");
		}
		
		public Argument returnType() {
			return new Argument("The entity id of the projectile", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The player to shoot the projectile from", CInt.class, CString.class, "playerOrEntity").setOptionalDefaultNull(),
						new Argument("The projectile type", MCProjectileType.class, "projectile").setOptionalDefault(MCProjectileType.FIREBALL)
					);
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}

	}

	@api(environments = {CommandHelperEnvironment.class})
	public static class entities_in_radius extends EntityFunction {

		public String getName() {
			return "entities_in_radius";
		}

		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			MCPlayer p = env.getEnv(CommandHelperEnvironment.class).GetPlayer();

			MCLocation loc;
			int dist;
			List<String> types = new ArrayList<String>();

			if (!(args[0] instanceof CArray)) {
				throw new ConfigRuntimeException("Expecting an array at parameter 1 of entities_in_radius",
						ExceptionType.BadEntityException, t);
			}

			loc = ObjectGenerator.GetGenerator().location(args[0], p != null ? p.getWorld() : null, t);
			dist = args[1].primitive(t).castToInt32(t);

			if (args.length == 3) {
				if (args[2] instanceof CArray) {
					CArray ta = (CArray) args[2];
					for (int i = 0; i < ta.size(); i++) {
						types.add(ta.get(i, t).val());
					}
				} else {
					types.add(args[2].val());
				}

				types = prepareTypes(t, types);
			}

			// The idea and code comes from skore87 (http://forums.bukkit.org/members/skore87.105075/)
			// http://forums.bukkit.org/threads/getnearbyentities-of-a-location.101499/#post-1341141
			int chunkRadius = dist < 16 ? 1 : (dist - (dist % 16)) / 16;

			Set<Integer> eSet = new HashSet<Integer>();
			for (int chX = 0 - chunkRadius; chX <= chunkRadius; chX++) {
				for (int chZ = 0 - chunkRadius; chZ <= chunkRadius; chZ++) {
					for (MCEntity e : loc.getChunk().getEntities()) {
						if (!e.getWorld().equals(loc.getWorld())) {
							continue;
						}
						if (e.getLocation().distance(loc) <= dist && e.getLocation().getBlock() != loc.getBlock()) {
							if (types.isEmpty() || types.contains(e.getType().name())) {
								eSet.add(e.getEntityId());
							}
						}
					}
				}
			}
			CArray entities = new CArray(t);
			for(int e : eSet){
				entities.push(new CInt(e, t));
			}

			return entities;
		}

		private List<String> prepareTypes(Target t, List<String> types) {

			List<String> newTypes = new ArrayList<String>();
			MCEntityType entityType = null;

			for (String type : types) {

				try {
					entityType = MCEntityType.valueOf(type.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new ConfigRuntimeException(String.format("Wrong entity type: %s", type),
							ExceptionType.BadEntityException, t);
				}

				newTypes.add(entityType.name());
			}

			return newTypes;
		}

		public String docs() {
			return "array {location array, distance, [type] | location array, distance, [arrayTypes]} Returns an array of"
					+ " all entities within the given radius. Set type argument to filter entities to a specific type. You"
					+ " can pass an array of types. Valid types (case doesn't matter): "
					+ StringUtils.Join(MCEntityType.values(), ", ", ", or ", " or ");
		}
		
		public Argument returnType() {
			return new Argument("An array of all entities in the given radius", CArray.class).setGenerics(new Generic(CInt.class));
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The center location", MLocation.class, "locationArray"),
						new Argument("The radius around the location to search", CNumber.class, "radius"),
						new Argument("The entity types", CArray.class, "type").setVarargs().setGenerics(new Generic(MCEntityType.class))
					);
		}

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.BadEntityException,
					ExceptionType.FormatException};
		}
	}

	//@api
	public static class get_mob_target extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity le = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			if (le.getTarget(t) == null) {
				return Construct.GetNullConstruct(CInt.class, t);
			} else {
				return new CInt(le.getTarget(t).getEntityId(), t);
			}
		}

		public String getName() {
			return "get_mob_target";
		}

		public String docs() {
			return "Gets the mob's target if it has one, and returns the target's entityID."
					+ " If there is no target, null is returned instead.";
		}
		
		public Argument returnType() {
			return new Argument("The entity id of the entity's target, or null if none", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}
	}

	//@api
	public static class set_mob_target extends EntitySetterFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.BadEntityException, ExceptionType.CastException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity le = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			MCLivingEntity target = null;
			if (!args[1].isNull()) {
				target = Static.getLivingEntity(args[1].primitive(t).castToInt32(t), t);
			}
			le.setTarget(target, t);
			return new CVoid(t);
		}

		public String getName() {
			return "set_mob_target";
		}

		public String docs() {
			return "Sets the mob's target. The first ID is the entity who is targetting, the second is the target."
					+ " It can also be set to null to clear the current target.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity to change the target of", CInt.class, "entityID1"),
						new Argument("The entity to change the target to", CInt.class, "entityID2")
					);
		}
	}

	@api
	public static class get_mob_equipment extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity le = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			Map<MCEquipmentSlot, MCItemStack> eq = le.getEquipment().getAllEquipment();
			CArray ret = CArray.GetAssociativeArray(t);
			for (MCEquipmentSlot key : eq.keySet()) {
				ret.set(key.name().toLowerCase(), ObjectGenerator.GetGenerator().item(eq.get(key), t), t);
			}
			return ret;
		}

		public String getName() {
			return "get_mob_equipment";
		}

		public String docs() {
			return "Returns an Equipment object showing the equipment this entity is wearing.";
		}
		
		public Argument returnType() {
			return new Argument("The equipment that the entity is currently wearing", MEquipment.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
					new ExampleScript("Getting a mob's equipment", "get_mob_equipment(276)", "{boots: null,"
							+ " chestplate: null, helmet: {data: 0, enchants: {} meta: null, type: 91}, leggings: null,"
							+ " weapon: {data: 5, enchants: {} meta: {display: Excalibur, lore: null}, type: 276}}")
			};
		}
	}

	@api
	public static class set_mob_equipment extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntityEquipment ee = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t).getEquipment();
			Map<MCEquipmentSlot, MCItemStack> eq = ee.getAllEquipment();
			if (args[1].isNull()) {
				ee.clearEquipment();
				return new CVoid(t);
			} else if (args[1] instanceof CArray) {
				CArray ea = (CArray) args[1];
				for (String key : ea.keySet()) {
					try {
						eq.put(MCEquipmentSlot.valueOf(key.toUpperCase()), ObjectGenerator.GetGenerator().item(ea.get(key, t), t));
					} catch (IllegalArgumentException iae) {
						throw new Exceptions.FormatException("Not an equipment slot: " + key, t);
					}
				}
			} else {
				throw new Exceptions.FormatException("Expected argument 2 to be an array", t);
			}
			ee.setAllEquipment(eq);
			return new CVoid(t);
		}

		public String getName() {
			return "set_mob_equipment";
		}

		public String docs() {
			return "Takes an Equipment object, with keys representing equipment slots and values"
					+ " of ItemStacks, and sets the entity's equipment to those items.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", MEquipment.class, "equipment")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{
				new ExampleScript("Basic usage", "set_mob_equipment(spawn_mob('SKELETON')[0], array(WEAPON: array(type: 261)))", "Gives a bow to a skeleton")
			};
		}
	}

	@api
	public static class get_max_health extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity le = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			return new CInt(le.getMaxHealth(), t);
		}

		public String getName() {
			return "get_max_health";
		}

		public String docs() {
			return "Returns the maximum health of this living entity.";
		}
		
		public Argument returnType() {
			return new Argument("", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityId")
					);
		}

	}

	@api
	public static class set_max_health extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCLivingEntity le = Static.getLivingEntity(args[0].primitive(t).castToInt32(t), t);
			le.setMaxHealth(args[1].primitive(t).castToInt32(t));
			return new CVoid(t);
		}

		public String getName() {
			return "set_max_health";
		}

		public String docs() {
			return "Sets the max health of this living entity."
					+ " this is persistent for players, and will not reset even"
					+ " after server restarts.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", CInt.class, "max")
					);
		}

		@Override
		public ExampleScript[] examples() throws ConfigCompileException {
			return new ExampleScript[]{new ExampleScript("Basic use",
					"set_max_health(256, 10)", "The entity will now only have 5 hearts max.")};
		}
	}

	@api
	public static class entity_onfire extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			int id = list.getInt("entityID", t);
			MCEntity ent = Static.getEntity(id, t);
			return new CInt(Static.ticksToMs(ent.getFireTicks())/1000, t);
		}

		public String getName() {
			return "entity_onfire";
		}

		public String docs() {
			return "Returns the number of seconds until this entity"
					+ " stops being on fire, 0 if it already isn't.";
		}
		
		public Argument returnType() {
			return new Argument("", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}

	}

	@api
	public static class set_entity_onfire extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			int id = list.getInt("entityID", t);
			int seconds = list.getInt("seconds", t);
			MCEntity ent = Static.getEntity(id, t);
			int setTicks = (int) Static.msToTicks(seconds * 1000);
			ent.setFireTicks(setTicks);
			return new CVoid(t);
		}

		public String getName() {
			return "set_entity_onfire";
		}

		public String docs() {
			return "Sets the entity on fire for the"
					+ " given number of seconds.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", CInt.class, "seconds").addAnnotation(new Ranged(0, true, Integer.MAX_VALUE, true))
					);
		}

	}

	@api
	public static class play_entity_effect extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			int id = list.getInt("entityID", t);
			MCEntityEffect mee = list.getEnum("effect", MCEntityEffect.class);
			MCEntity ent = Static.getEntity(id, t);
			ent.playEffect(mee);
			return new CVoid(t);
		}

		public String getName() {
			return "play_entity_effect";
		}

		public String docs() {
			return "Plays the given visual effect on the"
					+ " entity. Non-applicable effects simply won't happen. Note:"
					+ " the death effect makes the mob invisible to players and"
					+ " immune to melee attacks. When used on players, they are"
					+ " shown the respawn menu, but because they are not actually"
					+ " dead, they can only log out.";
		}
		
		public Argument returnType() {
			return Argument.VOID;
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID"),
						new Argument("", MCEntityEffect.class, "effect").addAnnotation(new NonNull())
					);
		}

	}
	
	@api
	public static class get_mob_name extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			int entityID = list.getInt("entityID", t);
			MCLivingEntity le = Static.getLivingEntity(entityID, t);
			return new CString(le.getCustomName(), t);
		}

		public String getName() {
			return "get_mob_name";
		}

		public String docs() {
			return "Returns the name of the given mob.";
		}
		
		public Argument returnType() {
			return new Argument("The name of the given mob", CString.class);
		}
		
		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity ID", CInt.class, "entityID")
					);
		}
	}
	
	@api
	public static class set_mob_name extends EntitySetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			ArgList list = getBuilder().parse(args, this, t);
			int entityID = list.getInt("entityID", t);
			String name = list.getString("name", t);
			MCLivingEntity le = Static.getLivingEntity(entityID, t);
			le.setCustomName(name);
			return new CVoid(t);
		}

		public String getName() {
			return "set_mob_name";
		}

		public String docs() {
			return "Sets the name of the given mob. This"
					+ " automatically truncates if it is more than 64 characters.";
		}

		public Argument returnType() {
			return Argument.VOID;
		}
		
		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity ID", CInt.class, "entityID"),
						new Argument("The name to give the entity", CString.class, "name").addAnnotation(new FormatString(".{0,64}"))
					);
		}
	}
	
	@api(environments = {CommandHelperEnvironment.class})
	public static class spawn_entity extends EntityFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.FormatException,
					ExceptionType.BadEntityException, ExceptionType.InvalidWorldException,
					ExceptionType.PlayerOfflineException};
		}

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			CArray ret = new CArray(t);
			ArgList list = getBuilder().parse(args, this, t);
			MCEntityType entType = list.getEnum("entityType", MCEntityType.class);
			int qty = list.getInt("qty", t);
			MLocation ml = list.get("location");
			MCLocation l = null;
			if(ml != null){
				l = ml.getMCLocation();
			}
			if(l == null){
				MCCommandSender cs = environment.getEnv(CommandHelperEnvironment.class).GetCommandSender();
				if (cs instanceof MCPlayer) {
					l = ((MCPlayer) cs).getLocation();
				} else if (cs instanceof MCBlockCommandSender){
					l = ((MCBlockCommandSender) cs).getBlock().getRelative(MCBlockFace.UP).getLocation();
				} else {
					throw new ConfigRuntimeException("A physical commandsender must exist or location must be explicit.",
							ExceptionType.PlayerOfflineException, t);
				}
			}
			for (int i = 0; i < qty; i++) {
				MCEntity ent;
				switch (entType) {
					case DROPPED_ITEM:
						CArray c = new CArray(t);
						c.set("type", new CInt(1, t), t);
						c.set("qty", new CInt(qty, t), t);
						MCItemStack is = ObjectGenerator.GetGenerator().item(c, t);
						ent = l.getWorld().dropItem(l, is);
						qty = 0;
						break;
					case FALLING_BLOCK:
						ent = l.getWorld().spawnFallingBlock(l, 12, (byte) 0);
						break;
					default:
						ent = l.getWorld().spawn(l, entType);
				}
				ret.push(new CInt(ent.getEntityId(), t));
			}
			return ret;
		}

		public String getName() {
			return "spawn_entity";
		}

		public Integer[] numArgs() {
			return new Integer[]{1, 2, 3};
		}

		public String docs() {
			List<String> spawnable = new ArrayList<String>();
			for (MCEntityType type : MCEntityType.values()) {
				if (type.isSpawnable()) {
					spawnable.add(type.name());
				}
			}
			return "Spawns the specified number of entities of the given type"
					+ " at the given location. Returns an array of entityIDs of what is spawned. Qty defaults to 1"
					+ " and location defaults to the location of the commandsender, if it is a block or player."
					+ " If the commandsender is console, location must be supplied. ---- Entitytype can be one of " 
					+ StringUtils.Join(spawnable, ", ", " or ", ", or ") 
					+ ". Falling_blocks will be sand by default, and dropped_items will be stone,"
					+ " as these entities already have their own functions for spawning.";
		}
		
		public Argument returnType() {
			return new Argument("The entityIDs of the spawned entities", CArray.class).setGenerics(new Generic(CInt.class));
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("The entity type. Note that only the spawnable types are valid", MCEntityType.class, "entityType"),
						new Argument("", CInt.class, "qty").setOptionalDefault(1).addAnnotation(Ranged.POSITIVE).addAnnotation(new NonNull()),
						new Argument("", MLocation.class, "location").setOptionalDefaultNull()
					);
		}
	}

	@api
	public static class set_entity_rider extends EntitySetterFunction {
	
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity horse, rider;
			boolean success;
			if (args[0].isNull()) {
				horse = null;
			} else {
				horse = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			}
			if (args[1].isNull()) {
				rider = null;
			} else {
				rider = Static.getEntity(args[1].primitive(t).castToInt32(t), t);
			}
			if ((horse == null && rider == null) || horse == rider) {
				throw new Exceptions.FormatException("Horse and rider cannot be the same entity", t);
			} else if (horse == null) {
				success = rider.leaveVehicle();
			} else if (rider == null) {
				success = horse.eject();
			} else {
				success = horse.setPassenger(rider);
			}
			return new CBoolean(success, t);
		}
	
		public String getName() {
			return "set_entity_rider";
		}
	
		public String docs() {
			return "Sets the way two entities are stacked. Horse and rider are entity ids."
					+ " If rider is null, horse will eject its current rider, if it has one. If horse is null,"
					+ " rider will leave whatever it is riding. If horse and rider are both valid entities,"
					+ " rider will ride horse. The function returns the success of whatever operation is done."
					+ " If horse and rider are both null, or otherwise the same, a FormatException is thrown.";
		}

		public Argument returnType() {
			return new Argument("", CBoolean.class);
		}
		
		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "horse"),
						new Argument("", CInt.class, "rider")
					);
		}
		
	}
	
	@api
	public static class get_entity_rider extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity ent = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			if (ent.getPassenger() instanceof MCEntity) {
				return new CInt(ent.getPassenger().getEntityId(), t);
			}
			return null;
		}

		public String getName() {
			return "get_entity_rider";
		}

		public String docs() {
			return "Returns the ID of the given entity's rider, or null if it doesn't have one.";
		}
		
		public Argument returnType() {
			return new Argument("", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}
	}
	
	@api
	public static class get_entity_vehicle extends EntityGetterFunction {

		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			MCEntity ent = Static.getEntity(args[0].primitive(t).castToInt32(t), t);
			if (ent.isInsideVehicle()) {
				return new CInt(ent.getVehicle().getEntityId(), t);
			}
			return Construct.GetNullConstruct(CInt.class, t);
		}

		public String getName() {
			return "get_entity_vehicle";
		}

		public String docs() {
			return "Returns the ID of the given entity's vehicle, or null if it doesn't have one.";
		}

		public Argument returnType() {
			return new Argument("", CInt.class);
		}

		public ArgumentBuilder arguments() {
			return ArgumentBuilder.Build(
						new Argument("", CInt.class, "entityID")
					);
		}
		
	}

	@api
	public static class set_entity_rider extends EntitySetterFunction {
	
		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			MCEntity horse, rider;
			boolean success;
			if (args[0] instanceof CNull) {
				horse = null;
			} else {
				horse = Static.getEntity(Static.getInt32(args[0], t), t);
			}
			if (args[1] instanceof CNull) {
				rider = null;
			} else {
				rider = Static.getEntity(Static.getInt32(args[1], t), t);
			}
			if ((horse == null && rider == null) || horse == rider) {
				throw new Exceptions.FormatException("Horse and rider cannot be the same entity", t);
			} else if (horse == null) {
				success = rider.leaveVehicle();
			} else if (rider == null) {
				success = horse.eject();
			} else {
				success = horse.setPassenger(rider);
			}
			return new CBoolean(success, t);
		}
	
		public String getName() {
			return "set_entity_rider";
		}
	
		public String docs() {
			return "boolean {horse, rider} Sets the way two entities are stacked. Horse and rider are entity ids."
					+ " If rider is null, horse will eject its current rider, if it has one. If horse is null,"
					+ " rider will leave whatever it is riding. If horse and rider are both valid entities,"
					+ " rider will ride horse. The function returns the success of whatever operation is done."
					+ " If horse and rider are both null, or otherwise the same, a FormatException is thrown.";
		}
	}
	
	@api
	public static class get_entity_rider extends EntityGetterFunction {

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			MCEntity ent = Static.getEntity(Static.getInt32(args[0], t), t);
			if (ent.getPassenger() instanceof MCEntity) {
				return new CInt(ent.getPassenger().getEntityId(), t);
			}
			return null;
		}

		public String getName() {
			return "get_entity_rider";
		}

		public String docs() {
			return "mixed {entityID} Returns the ID of the given entity's rider, or null if it doesn't have one.";
		}
	}
	
	@api
	public static class get_entity_vehicle extends EntityGetterFunction {

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			MCEntity ent = Static.getEntity(Static.getInt32(args[0], t), t);
			if (ent.isInsideVehicle()) {
				return new CInt(ent.getVehicle().getEntityId(), t);
			}
			return new CNull(t);
		}

		public String getName() {
			return "get_entity_vehicle";
		}

		public String docs() {
			return "mixed {entityID} Returns the ID of the given entity's vehicle, or null if it doesn't have one.";
		}
	}
}

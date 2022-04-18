package gdavid.phi.capability;

import java.util.ArrayList;
import java.util.List;

import gdavid.phi.Phi;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import vazkii.psi.api.internal.Vector3;

@EventBusSubscriber
public class AccelerationCapability implements IAccelerationCapability, INBTSerializable<CompoundNBT> {
	
	static final String tagAccelerations = "accelerations";
	static final String tagDuration = "duration";
	
	List<Acceleration> accelerations = new ArrayList<>();
	
	@Override
	public Vector3 getAcceleration() {
		Vector3 res = new Vector3();
		for (Acceleration a : accelerations) res.add(a.value);
		return res;
	}
	
	@Override
	public void addAcceleration(Vector3 acceleration, int duration) {
		accelerations.add(new Acceleration(acceleration, duration));
	}
	
	@Override
	public void tick(Entity entity) {
		if (entity.world.isRemote) return;
		Vector3 acc = getAcceleration();
		entity.addVelocity(acc.x, acc.y, acc.z);
		if (Math.abs(acc.y) > 0.0001) {
			if (entity.getMotion().getY() >= 0) {
				entity.fallDistance = 0;
			} else if (acc.y > 0) {
				double invTermVel = 25 / 98.0;
				double y = entity.getMotion().getY() * invTermVel + 1;
				if (y > 0) entity.fallDistance = (float) Math.min(entity.fallDistance, Math.max(0, (-(49 / invTermVel) + (((49 * y) - (Math.log(y) / Math.log(4 * invTermVel))) / invTermVel))));
			}
		}
		for (int i = accelerations.size() - 1; i >= 0; i--) {
			if (--accelerations.get(i).duration <= 0) accelerations.remove(i);
		}
	}
	
	@SubscribeEvent
	public static void attach(AttachCapabilitiesEvent<Entity> event) {
		event.addCapability(new ResourceLocation(Phi.modId, "acceleration"), new ModCapabilities.Provider<>(ModCapabilities.acceleration, new AccelerationCapability()));
	}
	
	@SubscribeEvent
	public static void onTick(TickEvent.WorldTickEvent event) {
		if (event.phase != Phase.START || !(event.world instanceof ServerWorld)) return;
		((ServerWorld) event.world).getEntities().forEach(entity -> {
			entity.getCapability(ModCapabilities.acceleration).ifPresent(cap -> cap.tick(entity));
		});
	}
	
	@Override
	public CompoundNBT serializeNBT() {
		CompoundNBT nbt = new CompoundNBT();
		ListNBT acc = new ListNBT();
		for (Acceleration a : accelerations) {
			CompoundNBT elem = new CompoundNBT();
			elem.putDouble("x", a.value.x);
			elem.putDouble("y", a.value.y);
			elem.putDouble("z", a.value.z);
			elem.putInt(tagDuration, a.duration);
			acc.add(elem);
		}
		nbt.put(tagAccelerations, acc);
		return nbt;
	}
	
	@Override
	public void deserializeNBT(CompoundNBT nbt) {
		ListNBT acc = nbt.getList(tagAccelerations, Constants.NBT.TAG_COMPOUND);
		accelerations = new ArrayList<>();
		for (int i = 0; i < acc.size(); i++) {
			CompoundNBT elem = acc.getCompound(i);
			accelerations.add(new Acceleration(new Vector3(elem.getDouble("x"),
					elem.getDouble("y"), elem.getDouble("z")), elem.getInt(tagDuration)));
		}
	}
	
	static class Acceleration {
		
		Vector3 value;
		int duration;
		
		Acceleration(Vector3 value, int duration) {
			this.value = value;
			this.duration = duration;
		}
		
	}
	
}

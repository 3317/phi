package gdavid.phi.spell.trick.blink;

import gdavid.phi.spell.ModPieces;
import gdavid.phi.util.ParamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import vazkii.psi.api.spell.EnumSpellStat;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.api.spell.SpellCompilationException;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.api.spell.SpellMetadata;
import vazkii.psi.api.spell.SpellParam;
import vazkii.psi.api.spell.SpellRuntimeException;
import vazkii.psi.api.spell.param.ParamEntity;
import vazkii.psi.api.spell.param.ParamNumber;
import vazkii.psi.api.spell.piece.PieceTrick;

public class SwapBlinkTrick extends PieceTrick {
	
	SpellParam<Entity> a, b;
	SpellParam<Number> distance;
	
	public SwapBlinkTrick(Spell spell) {
		super(spell);
	}
	
	@Override
	public void initParams() {
		addParam(a = new ParamEntity(ModPieces.Params.target1, SpellParam.YELLOW, false, false));
		addParam(b = new ParamEntity(ModPieces.Params.target2, SpellParam.YELLOW, true, false));
		addParam(distance = new ParamNumber(SpellParam.GENERIC_NAME_DISTANCE, SpellParam.RED, false, true));
	}
	
	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
		super.addToMetadata(meta);
		double maxDistance = ParamHelper.positive(this, distance);
		meta.addStat(EnumSpellStat.POTENCY, (int) (Math.sqrt(2 * maxDistance) * 40));
		meta.addStat(EnumSpellStat.COST, (int) (maxDistance * 40));
	}
	
	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		double distanceVal = getNonnullParamValue(context, distance).doubleValue();
		Entity e1 = getNonnullParamValue(context, a);
		Entity e2 = getParamValueOrDefault(context, b, context.caster);
		context.verifyEntity(e1);
		context.verifyEntity(e2);
		if (!context.isInRadius(e1) || !context.isInRadius(e2)) {
			throw new SpellRuntimeException(SpellRuntimeException.OUTSIDE_RADIUS);
		}
		Vector3d offset = e2.getPositionVec().subtract(e1.getPositionVec());
		if (offset.lengthSquared() > distanceVal * distanceVal) return null;
		Vector3d pos1 = e1.getPositionVec();
		Vector2f rot1 = e1.getPitchYaw();
		e1.setPosition(e2.getPosX(), e2.getPosY(), e2.getPosZ());
		e1.rotationYaw = e1.prevRotationYaw = e2.rotationYaw;
		e1.rotationPitch = e1.prevRotationPitch = e2.rotationPitch;
		e2.setPosition(pos1.x, pos1.y, pos1.z);
		e1.rotationYaw = e1.prevRotationYaw = rot1.x;
		e1.rotationPitch = e1.prevRotationPitch = rot1.y;
		if (e1 instanceof PlayerEntity) {
			try {
				Object message = Class.forName("vazkii.psi.common.network.message.MessageBlink")
						.getConstructor(double.class, double.class, double.class)
						.newInstance(offset.x, offset.y, offset.z);
				Class.forName("vazkii.psi.common.network.MessageRegister")
						.getMethod("sendToPlayer", Object.class, PlayerEntity.class).invoke(null, message, e1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (e2 instanceof PlayerEntity) {
			try {
				Object message = Class.forName("vazkii.psi.common.network.message.MessageBlink")
						.getConstructor(double.class, double.class, double.class)
						.newInstance(-offset.x, -offset.y, -offset.z);
				Class.forName("vazkii.psi.common.network.MessageRegister")
						.getMethod("sendToPlayer", Object.class, PlayerEntity.class).invoke(null, message, e2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
}
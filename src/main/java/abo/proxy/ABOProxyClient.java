package abo.proxy;

import abo.ABO;
import abo.energy.TileWaterwheel;
import abo.energy.TileWindmill;
import abo.pipes.fluids.PipeTransportFluidsReinforced;
import abo.render.RenderWaterwheel;
import abo.render.RenderWindmill;
import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.render.PipeTransportRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import buildcraft.core.render.RenderingEntityBlocks;
import buildcraft.core.render.RenderingEntityBlocks.EntityRenderIndex;
import buildcraft.transport.TransportProxyClient;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import da3dsoul.scaryGen.entity.EntityItemBat;
import da3dsoul.scaryGen.render.RenderBat;

public class ABOProxyClient extends ABOProxy {
	@Override
	public void registerPipe(Item itemID) {
		super.registerPipe(itemID);

		MinecraftForgeClient.registerItemRenderer(itemID, TransportProxyClient.pipeItemRenderer);
	}

	public void registerTileEntities() {
		super.registerTileEntities();
		ClientRegistry.bindTileEntitySpecialRenderer(TileWindmill.class, new RenderWindmill());
		ClientRegistry.bindTileEntitySpecialRenderer(TileWaterwheel.class, new RenderWaterwheel());

	}

	public void registerBlockRenderers() {
		RenderingEntityBlocks.blockByEntityRenders.put(new EntityRenderIndex(ABO.windmillBlock, 0),
				new RenderWindmill());

        PipeTransportRenderer renderer = PipeTransportRenderer.RENDERER_MAP.get(PipeTransportFluids.class);

        PipeTransportRenderer.RENDERER_MAP.put(PipeTransportFluidsReinforced.class, renderer);

		//RenderingEntityBlocks.blockByEntityRenders.put(new EntityRenderIndex(ABO.waterwheelBlock, 0),
		//		new RenderWaterwheel());
	}
	
	public void registerEntities()
	{
		RenderingRegistry.registerEntityRenderingHandler(EntityItemBat.class, new RenderBat());
	}
}

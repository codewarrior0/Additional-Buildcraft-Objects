package abo.pipes.items;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import buildcraft.api.tools.IToolWrench;
import buildcraft.core.ItemWrench;
import buildcraft.core.lib.inventory.InvUtils;
import buildcraft.core.lib.inventory.Transactor;
import buildcraft.core.lib.inventory.TransactorSimple;
import buildcraft.transport.pipes.PipeItemsEmerald;
import buildcraft.transport.pipes.PipeItemsIron;
import buildcraft.transport.pipes.PipeItemsWood;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraftforge.common.util.ForgeDirection;
import abo.ABO;
import abo.PipeIcons;
import abo.pipes.ABOPipe;
import buildcraft.api.transport.IPipeTile;
import buildcraft.transport.PipeTransportItems;
import buildcraft.transport.TileGenericPipe;
import buildcraft.transport.TravelingItem;
import buildcraft.transport.pipes.events.PipeEventItem;

public class PipeItemsInsertion extends ABOPipe<PipeTransportItems> {

    public boolean isLegacy = false;

    public PipeItemsInsertion(Item itemID) {
        super(new PipeTransportItems(), itemID);

        transport.allowBouncing = true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setBoolean("isLegacy", isLegacy);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        if(nbttagcompound.hasKey("isLegacy")) {
            isLegacy = nbttagcompound.getBoolean("isLegacy");
        } else {
            isLegacy = true;
        }
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return isLegacy ? PipeIcons.PipeItemsInsertionLegacy.ordinal() : PipeIcons.PipeItemsInsertionNew.ordinal();
    }

    @Override
    public boolean blockActivated(EntityPlayer entityplayer, ForgeDirection hitSide) {
        if (entityplayer.getCurrentEquippedItem() != null
                && entityplayer.getCurrentEquippedItem().getItem() instanceof IToolWrench && ((IToolWrench)entityplayer.getCurrentEquippedItem().getItem()).canWrench(entityplayer, container.xCoord, container.yCoord, container.zCoord)) {
            isLegacy = !isLegacy;
            container.scheduleRenderUpdate();
            ((IToolWrench)entityplayer.getCurrentEquippedItem().getItem()).wrenchUsed(entityplayer, container.xCoord, container.yCoord, container.zCoord);
            return true;
        }
        TileEntity tile = null;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            tile = getWorld().getTileEntity(container.xCoord + side.offsetX, container.yCoord + side.offsetY,
                    container.zCoord + side.offsetZ);
            if (tile != null && tile instanceof TileEntityEnderChest) break;
        }
        if (tile == null || !(tile instanceof TileEntityEnderChest)) { return false; }
        if (getWorld().isRemote) {
            return true;
        } else {
            ABO.instance.getInventoryEnderChest().func_146031_a((TileEntityEnderChest) tile);
            entityplayer.displayGUIChest(ABO.instance.getInventoryEnderChest());
            return true;
        }
    }



    public void eventHandler(PipeEventItem.FindDest event) {
        LinkedList<ForgeDirection> nonPipesList = new LinkedList<ForgeDirection>();
        LinkedList<ForgeDirection> pipesList = new LinkedList<ForgeDirection>();

        List<ForgeDirection> result = event.destinations;
        TravelingItem item = event.item;

        result.clear();

        EnumSet<ForgeDirection> sides = EnumSet.complementOf(item.blacklist);
        sides.remove(ForgeDirection.UNKNOWN);

        for (ForgeDirection o : sides) {
            if (outputOpen(o) && canReceivePipeObjects(o, item)) {
                result.add(o);
            }
        }

        for (ForgeDirection o : result) {
            TileEntity entity = container.getTile(o);
            if (entity instanceof IPipeTile) {
                pipesList.add(o);
            }else{
                    nonPipesList.add(o);
            }
        }

        if (!nonPipesList.isEmpty()) {
            result.clear();
            result.addAll(nonPipesList);
            return;
        }
        result.clear();
        result.addAll(pipesList);
        return;
    }

    @Override
    public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
        if (tile instanceof TileEntityEnderChest) return true;
        return super.canPipeConnect(tile, side);
    }

    public void eventHandler(PipeEventItem.ReachedEnd event) {
        TileEntity tile = null;
        TravelingItem item = event.item;
        tile = event.dest;
        if (tile == null) { return; }
        if (tile instanceof TileEntityEnderChest) {
            if (!container.getWorldObj().isRemote) {
                ItemStack added = new TransactorSimple(InvUtils.getInventory((IInventory) ABO.instance
                        .getInventoryEnderChest())).add(item.getItemStack(), item.output.getOpposite(), true);
                item.getItemStack().stackSize -= added.stackSize;

                if (item.getItemStack().stackSize <= 0) {
                    transport.items.scheduleRemoval(item);
                }
                event.handled = true;
            }
        }
    }

    private boolean canReceivePipeObjects(ForgeDirection o, TravelingItem item) {
        TileEntity entity = container.getTile(o);

        if (!container.isPipeConnected(o)) { return false; }

        if (entity instanceof TileGenericPipe) {
            TileGenericPipe pipe = (TileGenericPipe) entity;
            if(pipe.pipe == null || pipe.pipe.transport == null) return false;

            boolean iron = pipe.pipe instanceof PipeItemsIron && pipe.pipe.outputOpen(o.getOpposite());

            return isLegacy ? (pipe.pipe.transport instanceof PipeTransportItems) : (pipe.pipe.transport instanceof PipeTransportItems && pipe.pipe.inputOpen(o.getOpposite()) && pipe.canInjectItems(o.getOpposite()) && !iron);
		} else if (entity instanceof IInventory && item.getInsertionHandler().canInsertItem(item, (IInventory) entity)) {
			if (Transactor.getTransactorFor(entity).add(item.getItemStack(), o.getOpposite(), false).stackSize > 0) { return true; }
        } else if (entity instanceof TileEntityEnderChest) {
            if (new TransactorSimple(InvUtils.getInventory((IInventory) ABO.instance.getInventoryEnderChest())).add(
                    item.getItemStack(), o.getOpposite(), false).stackSize > 0) { return true; }
        }

        return false;
    }

}
package alz.mods.enhancedportals.portals;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import alz.mods.enhancedportals.common.WorldLocation;
import alz.mods.enhancedportals.helpers.WorldHelper;
import alz.mods.enhancedportals.reference.Localizations;
import alz.mods.enhancedportals.reference.Reference;
import alz.mods.enhancedportals.reference.Settings;
import alz.mods.enhancedportals.reference.Strings;
import alz.mods.enhancedportals.tileentity.TileEntityNetherPortal;
import alz.mods.enhancedportals.tileentity.TileEntityPortalModifier;

public class PortalHandler
{
    public static class Create
    {
        static final int MAX_CHANCES = 10;

        /***
         * Creates a portal at the specified location
         * 
         * @param location
         *            The WorldLocation to create the portal at
         * @return
         */
        public static boolean createPortal(WorldLocation location)
        {
            return createPortal(location, PortalTexture.PURPLE);
        }

        /***
         * Creates a portal at the specified location
         * 
         * @param location
         *            The WorldLocation to create the portal at
         * @param shape
         *            The shape of the portal
         * @return
         */
        public static boolean createPortal(WorldLocation location, PortalShape shape)
        {
            return createPortal(location, shape, PortalTexture.PURPLE);
        }

        /***
         * Creates a portal at the specified location
         * 
         * @param location
         *            The WorldLocation to create the portal at
         * @param shape
         *            The shape of the portal
         * @param texture
         *            The texture of the portal
         * @return
         */
        public static boolean createPortal(WorldLocation location, PortalShape shape, PortalTexture texture)
        {
            return createPortal(location, shape, texture, null);
        }
        
        public static boolean createPortal(WorldLocation location, PortalShape shape, PortalTexture texture, TileEntityPortalModifier modifier)
        {
            if (shape == PortalShape.UNKNOWN || location.worldObj == null || location.worldObj.isRemote || !isBlockRemovable(location.getBlockId()))
            {
                return false;
            }

            if (texture == null || texture == PortalTexture.UNKNOWN)
            {
                texture = PortalTexture.PURPLE;
            }

            Queue<WorldLocation> queue = new LinkedList<WorldLocation>();
            Queue<WorldLocation> addedBlocks = new LinkedList<WorldLocation>();
            int usedChances = 0;

            queue.add(location);

            while (!queue.isEmpty())
            {
                WorldLocation current = queue.remove();
                int currentBlockID = current.getBlockId();

                if (Settings.MaximumPortalSize > 0 && addedBlocks.size() >= Settings.MaximumPortalSize)
                {
                    Remove.removePortal(addedBlocks);
                    Reference.LogData(String.format(Localizations.getLocalizedString(Strings.Console_sizeFail), location.xCoord, location.yCoord, location.zCoord), location.worldObj.isRemote);
                    return false;
                }

                if (isBlockRemovable(currentBlockID))
                {
                    int sides = getSides(current, shape);

                    if (sides == -1)
                    {
                        Remove.removePortal(addedBlocks);
                        return false;
                    }
                    else if (sides < 2 && usedChances < MAX_CHANCES)
                    {
                        usedChances++;
                        sides += 2;
                    }

                    if (sides >= 2)
                    {
                        addedBlocks.add(current);
                        current.setBlock(Reference.BlockIDs.NetherPortal);
                        Data.updateTexture(current, texture, PortalTexture.UNKNOWN);
                        
                        if (modifier != null)
                        {
                            Data.setModifierParent(current, modifier);
                        }
                        
                        queue = updateQueue(queue, current, shape);

                        if (addedBlocks.size() == 1)
                        {
                            current.worldObj.setBlockMetadataWithNotify(current.xCoord, current.yCoord, current.zCoord, 1, 2);
                        }
                    }
                }
            }

            if (validatePortal(location, shape))
            {
                Reference.LogData(String.format(Localizations.getLocalizedString(Strings.Console_success), addedBlocks.size(), location.xCoord, location.yCoord, location.zCoord), location.worldObj.isRemote);

                return true;
            }
            else
            {
                Reference.LogData(String.format("%s " + Localizations.getLocalizedString(Strings.Console_fail), shape, addedBlocks.size(), location.xCoord, location.yCoord, location.zCoord), location.worldObj.isRemote);

                Remove.removePortal(addedBlocks);
                return false;
            }
        }

        /***
         * Creates a portal at the specified location
         * 
         * @param location
         *            The WorldLocation to create the portal at
         * @param texture
         *            The texture of the portal blocks
         * @return
         */
        public static boolean createPortal(WorldLocation location, PortalTexture texture)
        {
            if (createPortal(location, PortalShape.X, texture))
            {
                return true;
            }
            else if (createPortal(location, PortalShape.Z, texture))
            {
                return true;
            }
            else if (createPortal(location, PortalShape.HORIZONTAL, texture))
            {
                return true;
            }

            return false;
        }
        
        public static boolean createPortal(WorldLocation location, PortalTexture texture, TileEntityPortalModifier modifier)
        {
            if (createPortal(location, PortalShape.X, texture, modifier))
            {
                return true;
            }
            else if (createPortal(location, PortalShape.Z, texture, modifier))
            {
                return true;
            }
            else if (createPortal(location, PortalShape.HORIZONTAL, texture, modifier))
            {
                return true;
            }

            return false;
        }

        /***
         * Attempts to create a portal on all six sides of the block
         * 
         * @param location
         *            The WorldLocation of the block itself.
         * @return
         */
        public static boolean createPortalAroundBlock(WorldLocation location)
        {
            return createPortalAroundBlock(location, PortalTexture.PURPLE);
        }

        /***
         * Attempts to create a portal on all six sides of the block
         * 
         * @param location
         *            The WorldLocation of the block itself.
         * @param texture
         *            The texture to apply to the portal.
         * @return
         */
        public static boolean createPortalAroundBlock(WorldLocation location, PortalTexture texture)
        {
            if (createPortal(location.getOffset(ForgeDirection.UP), texture))
            {
                return true;
            }
            else if (createPortal(location.getOffset(ForgeDirection.DOWN), texture))
            {
                return true;
            }
            else if (createPortal(location.getOffset(ForgeDirection.NORTH), texture))
            {
                return true;
            }
            else if (createPortal(location.getOffset(ForgeDirection.SOUTH), texture))
            {
                return true;
            }
            else if (createPortal(location.getOffset(ForgeDirection.EAST), texture))
            {
                return true;
            }
            else if (createPortal(location.getOffset(ForgeDirection.WEST), texture))
            {
                return true;
            }

            return false;
        }

        /***
         * Creates a portal on the active face of the Portal Modifier.
         * 
         * @param modifier
         *            The Portal Modifier to create a portal on.
         * @return
         */
        public static boolean createPortalFromModifier(TileEntityPortalModifier modifier)
        {
            int[] offset = WorldHelper.offsetDirectionBased(modifier.worldObj, modifier.xCoord, modifier.yCoord, modifier.zCoord);

            return createPortal(new WorldLocation(modifier.worldObj, offset[0], offset[1], offset[2]), modifier.getTexture(), modifier);
        }

        private static int getSides(WorldLocation location, PortalShape shape)
        {
            int totalSides = 0;
            int[] allBlocks = new int[4];

            if (shape == PortalShape.HORIZONTAL)
            {
                allBlocks[0] = location.getOffset(ForgeDirection.NORTH).getBlockId();
                allBlocks[1] = location.getOffset(ForgeDirection.SOUTH).getBlockId();
                allBlocks[2] = location.getOffset(ForgeDirection.EAST).getBlockId();
                allBlocks[3] = location.getOffset(ForgeDirection.WEST).getBlockId();
            }
            else if (shape == PortalShape.X)
            {
                allBlocks[0] = location.getOffset(ForgeDirection.WEST).getBlockId();
                allBlocks[1] = location.getOffset(ForgeDirection.EAST).getBlockId();
                allBlocks[2] = location.getOffset(ForgeDirection.UP).getBlockId();
                allBlocks[3] = location.getOffset(ForgeDirection.DOWN).getBlockId();
            }
            else if (shape == PortalShape.Z)
            {
                allBlocks[0] = location.getOffset(ForgeDirection.NORTH).getBlockId();
                allBlocks[1] = location.getOffset(ForgeDirection.SOUTH).getBlockId();
                allBlocks[2] = location.getOffset(ForgeDirection.UP).getBlockId();
                allBlocks[3] = location.getOffset(ForgeDirection.DOWN).getBlockId();
            }

            for (int val : allBlocks)
            {
                if (isBlockFrame(val, true))
                {
                    totalSides++;
                }
                else if (!isBlockRemovable(val))
                {
                    return -1;
                }
            }

            return totalSides;
        }

        private static boolean validatePortal(WorldLocation location, PortalShape shape)
        {
            if (location.worldObj.isRemote)
            {
                return true;
            }

            Queue<WorldLocation> queue = new LinkedList<WorldLocation>();
            Queue<WorldLocation> checkedQueue = new LinkedList<WorldLocation>();
            queue.add(location);

            while (!queue.isEmpty())
            {
                WorldLocation current = queue.remove();
                int currentBlockID = current.getBlockId();

                if (currentBlockID == Reference.BlockIDs.NetherPortal && !queueContains(checkedQueue, current))
                {
                    int sides = getSides(current, shape);

                    if (sides != 4)
                    {
                        return false;
                    }

                    queue = updateQueue(queue, current, shape);
                    checkedQueue.add(current);
                }
            }

            return true;
        }
    }

    public static class Data
    {
        /***
         * Updates all the portal block's textures to the new one
         * 
         * @param location
         *            The location of the portal
         * @param newTexture
         *            The new texture to add
         * @param oldTexture
         *            The texture to replace, leave null/PortalTexture.UNKNOWN
         *            for all.
         * @param updateModifiers
         *            Should Portal Modifiers be updated too? (Only if
         *            oldTexture is set, will only update it if the modifier's
         *            texture equals oldTexture)
         * @return
         */
        public static boolean floodUpdateTexture(WorldLocation location, PortalTexture newTexture, PortalTexture oldTexture, boolean updateModifiers)
        {
            if (location.getBlockId() != Reference.BlockIDs.NetherPortal || oldTexture == null || oldTexture == PortalTexture.UNKNOWN || newTexture == oldTexture)
            {
                return false;
            }

            PortalShape shape = PortalShape.getPortalShape(location);
            Queue<WorldLocation> queue = new LinkedList<WorldLocation>();
            queue.add(location);

            while (!queue.isEmpty())
            {
                WorldLocation current = queue.remove();
                int currentBlockID = current.getBlockId();

                if (currentBlockID == Reference.BlockIDs.NetherPortal)
                {
                    PortalTexture texture = ((TileEntityNetherPortal) current.getBlockTileEntity()).Texture;

                    if (texture == oldTexture)
                    {
                        updateTexture(current, newTexture, oldTexture, false);
                        queue = updateQueue(queue, current, shape);
                    }
                }
                else if (updateModifiers && currentBlockID == Reference.BlockIDs.PortalModifier)
                {
                    TileEntityPortalModifier modifier = (TileEntityPortalModifier) current.getBlockTileEntity();

                    if (modifier.getTexture() == oldTexture) // This should only affect modifiers if they're the same texture as what's being replaced
                    {
                        modifier.setTexture(newTexture);

                        if (current.worldObj.isRemote)
                        {
                            current.markBlockForUpdate();
                        }
                        else
                        {
                            Reference.ServerHandler.sendUpdatePacketToNearbyClients(modifier);
                        }
                    }
                }
            }

            return true;
        }

        /***
         * Updates the portals texture from a modifier
         * 
         * @param modifier
         *            The modifier to update
         * @param newTexture
         *            The new texture to apply
         * @param updateSelf
         *            Should the portal modifier update itself?
         * @return
         */
        public static boolean floodUpdateTextureModifier(TileEntityPortalModifier modifier, PortalTexture newTexture, boolean updateSelf)
        {
            int[] offset = WorldHelper.offsetDirectionBased(modifier.worldObj, modifier.xCoord, modifier.yCoord, modifier.zCoord);
            PortalTexture oldTexture = modifier.getTexture();

            if (updateSelf)
            {
                modifier.setTexture(newTexture);

                if (!modifier.worldObj.isRemote)
                {
                    Reference.ServerHandler.sendUpdatePacketToNearbyClients(modifier);
                }
            }

            return floodUpdateTexture(new WorldLocation(modifier.worldObj, offset[0], offset[1], offset[2]), newTexture, oldTexture, false);
        }

        public static boolean updateTexture(WorldLocation location, PortalTexture newTexture, PortalTexture oldTexture)
        {
            return updateTexture(location, newTexture, oldTexture, true);
        }

        public static boolean updateTexture(WorldLocation location, PortalTexture newTexture, PortalTexture oldTexture, boolean sendPacket)
        {
            if (location.getBlockId() != Reference.BlockIDs.NetherPortal)
            {
                return false;
            }

            TileEntityNetherPortal netherPortal = (TileEntityNetherPortal) location.getBlockTileEntity();

            if (oldTexture != null && oldTexture != PortalTexture.UNKNOWN && netherPortal.Texture != oldTexture)
            {
                return false;
            }

            netherPortal.Texture = newTexture;

            if (location.worldObj.isRemote)
            {
                location.markBlockForUpdate();
            }
            else
            {
                if (location.getBlockMeta() == 1)
                {
                    Reference.ServerHandler.sendUpdatePacketToNearbyClients(netherPortal);
                }
            }

            return true;
        }
        
        public static void setModifierParent(WorldLocation portal, TileEntityPortalModifier modifier)
        {
            if (portal.worldObj.provider.dimensionId != modifier.worldObj.provider.dimensionId)
            {
                return;
            }
                        
            TileEntityNetherPortal netherPortal = (TileEntityNetherPortal) portal.getBlockTileEntity();
            netherPortal.ParentModifier = new WorldLocation(modifier.xCoord, modifier.yCoord, modifier.zCoord);
        }
    }

    public static class Remove
    {
        /***
         * Removes a portal at the specified location
         * 
         * @param location
         *            The location of the portal
         * @return
         */
        public static void removePortal(Queue<WorldLocation> addedBlocks)
        {
            while (!addedBlocks.isEmpty())
            {
                WorldLocation current = addedBlocks.remove();
                current.setBlockToAir();
            }
        }

        /***
         * Removes a portal at the specified location
         * 
         * @param location
         *            The location of the portal
         * @return
         */
        public static void removePortal(WorldLocation location)
        {
            removePortal(location, PortalShape.UNKNOWN);
        }

        /***
         * Removes a portal at the specified location
         * 
         * @param location
         *            The location of the portal
         * @return
         */
        public static void removePortal(WorldLocation location, PortalShape shape)
        {
            if (location.getBlockId() != Reference.BlockIDs.NetherPortal)
            {
                return;
            }

            Queue<WorldLocation> queue = new LinkedList<WorldLocation>();
            queue.add(location);

            while (!queue.isEmpty())
            {
                WorldLocation current = queue.remove();
                int currentBlockID = current.getBlockId();

                if (currentBlockID == Reference.BlockIDs.NetherPortal)
                {
                    current.setBlockToAir();
                    queue = updateQueue(queue, current, shape);
                }
            }
        }

        /***
         * Removes all portals around this block
         * 
         * @param location
         *            The location of the base block
         */
        public static void removePortalAround(WorldLocation location)
        {
            removePortal(location.getOffset(ForgeDirection.UP));
            removePortal(location.getOffset(ForgeDirection.DOWN));
            removePortal(location.getOffset(ForgeDirection.NORTH));
            removePortal(location.getOffset(ForgeDirection.SOUTH));
            removePortal(location.getOffset(ForgeDirection.EAST));
            removePortal(location.getOffset(ForgeDirection.WEST));
        }

        /***
         * Removes a portal from a portal modifier
         * 
         * @param modifier
         *            The portal modifier to remove the attached portal from
         * @return
         */
        public static void removePortalModifier(TileEntityPortalModifier modifier)
        {
            int[] offset = WorldHelper.offsetDirectionBased(modifier.worldObj, modifier.xCoord, modifier.yCoord, modifier.zCoord);

            removePortal(new WorldLocation(modifier.worldObj, offset[0], offset[1], offset[2]));
        }
    }

    public static class Shape
    {
        /***
         * Gets a portal's shape
         * 
         * @param world
         * @param x
         * @param y
         * @param z
         * @return
         */
        public static PortalShape getPortalShape(IBlockAccess world, int x, int y, int z)
        {
            if (isBlockFrame(world.getBlockId(x - 1, y, z), true) && isBlockFrame(world.getBlockId(x + 1, y, z), true) && isBlockFrame(world.getBlockId(x, y + 1, z), true) && isBlockFrame(world.getBlockId(x, y - 1, z), true))
            {
                return PortalShape.X;
            }
            else if (isBlockFrame(world.getBlockId(x, y, z - 1), true) && isBlockFrame(world.getBlockId(x, y, z + 1), true) && isBlockFrame(world.getBlockId(x, y + 1, z), true) && isBlockFrame(world.getBlockId(x, y - 1, z), true))
            {
                return PortalShape.Z;
            }
            else if (isBlockFrame(world.getBlockId(x - 1, y, z), true) && isBlockFrame(world.getBlockId(x + 1, y, z), true) && isBlockFrame(world.getBlockId(x, y, z + 1), true) && isBlockFrame(world.getBlockId(x, y, z - 1), true))
            {
                return PortalShape.HORIZONTAL;
            }

            return PortalShape.UNKNOWN;
        }

        /***
         * Gets a portal's shape
         * 
         * @param world
         * @param x
         * @param y
         * @param z
         * @return
         */
        public static PortalShape getPortalShape(World world, int x, int y, int z)
        {
            if (isBlockFrame(world.getBlockId(x - 1, y, z), true) && isBlockFrame(world.getBlockId(x + 1, y, z), true) && isBlockFrame(world.getBlockId(x, y + 1, z), true) && isBlockFrame(world.getBlockId(x, y - 1, z), true))
            {
                return PortalShape.X;
            }
            else if (isBlockFrame(world.getBlockId(x, y, z - 1), true) && isBlockFrame(world.getBlockId(x, y, z + 1), true) && isBlockFrame(world.getBlockId(x, y + 1, z), true) && isBlockFrame(world.getBlockId(x, y - 1, z), true))
            {
                return PortalShape.Z;
            }
            else if (isBlockFrame(world.getBlockId(x - 1, y, z), true) && isBlockFrame(world.getBlockId(x + 1, y, z), true) && isBlockFrame(world.getBlockId(x, y, z + 1), true) && isBlockFrame(world.getBlockId(x, y, z - 1), true))
            {
                return PortalShape.HORIZONTAL;
            }

            return PortalShape.UNKNOWN;
        }
    }

    public static boolean isBlockFrame(int ID, boolean includeSelf)
    {
        if (includeSelf && ID == Reference.BlockIDs.NetherPortal)
        {
            return true;
        }

        return Settings.BorderBlocks.contains(ID);
    }

    public static boolean isBlockRemovable(int ID)
    {
        return Settings.RemovableBlocks.contains(ID);
    }

    private static boolean queueContains(Queue<WorldLocation> queue, WorldLocation loc)
    {
        for (WorldLocation queueLoc : queue)
        {
            if (queueLoc.equals(loc))
            {
                return true;
            }
        }

        return false;
    }

    private static Queue<WorldLocation> updateQueue(Queue<WorldLocation> queue, WorldLocation location, PortalShape shape)
    {
        if (shape == PortalShape.X)
        {
            queue.add(location.getOffset(ForgeDirection.UP));
            queue.add(location.getOffset(ForgeDirection.DOWN));
            queue.add(location.getOffset(ForgeDirection.EAST));
            queue.add(location.getOffset(ForgeDirection.WEST));
        }
        else if (shape == PortalShape.Z)
        {
            queue.add(location.getOffset(ForgeDirection.UP));
            queue.add(location.getOffset(ForgeDirection.DOWN));
            queue.add(location.getOffset(ForgeDirection.NORTH));
            queue.add(location.getOffset(ForgeDirection.SOUTH));
        }
        else if (shape == PortalShape.HORIZONTAL)
        {
            queue.add(location.getOffset(ForgeDirection.NORTH));
            queue.add(location.getOffset(ForgeDirection.SOUTH));
            queue.add(location.getOffset(ForgeDirection.EAST));
            queue.add(location.getOffset(ForgeDirection.WEST));
        }
        else if (shape == PortalShape.UNKNOWN) // used for deconstructing portals
        {
            queue.add(location.getOffset(ForgeDirection.UP));
            queue.add(location.getOffset(ForgeDirection.DOWN));
            queue.add(location.getOffset(ForgeDirection.NORTH));
            queue.add(location.getOffset(ForgeDirection.SOUTH));
            queue.add(location.getOffset(ForgeDirection.EAST));
            queue.add(location.getOffset(ForgeDirection.WEST));
        }

        return queue;
    }
}

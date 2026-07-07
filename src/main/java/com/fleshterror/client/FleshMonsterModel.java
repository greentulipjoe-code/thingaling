package com.fleshterror.client;

import com.fleshterror.entity.FleshMonsterEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Body + six segmented, wiggling tentacles arranged radially. Whole thing is scaled
 * again per growth-stage by FleshMonsterRenderer, so the mesh itself only needs to
 * be modeled once at the "base" size.
 */
public class FleshMonsterModel extends HierarchicalModel<FleshMonsterEntity> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] tentacleBases;
    private final ModelPart[] tentacleTips;

    private static final int TENTACLE_COUNT = 6;

    public FleshMonsterModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tentacleBases = new ModelPart[TENTACLE_COUNT];
        this.tentacleTips = new ModelPart[TENTACLE_COUNT];
        for (int i = 0; i < TENTACLE_COUNT; i++) {
            ModelPart base = root.getChild("tentacle_base_" + i);
            this.tentacleBases[i] = base;
            this.tentacleTips[i] = base.getChild("tentacle_tip_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Central fleshy mass. UV: box 16x16x16 -> needs 64x32 region, texOffs(0,0).
        parts.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Six tentacles arranged evenly around the body, angled outward and slightly down.
        for (int i = 0; i < TENTACLE_COUNT; i++) {
            double angle = (Math.PI * 2 / TENTACLE_COUNT) * i;
            float x = (float) (Math.sin(angle) * 8.0);
            float z = (float) (Math.cos(angle) * 8.0);
            float yaw = (float) angle;

            int texX = (i % 4) * 16;
            int texY = 32 + (i / 4) * 16;

            PartDefinition base = parts.addOrReplaceChild("tentacle_base_" + i,
                    CubeListBuilder.create()
                            .texOffs(texX, texY)
                            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                    PartPose.offsetAndRotation(x, 20.0F, z, 0.4F, yaw, 0.0F));

            int tipTexX = (i % 4) * 16;
            int tipTexY = 64 + (i / 4) * 16;

            base.addOrReplaceChild("tentacle_tip_" + i,
                    CubeListBuilder.create()
                            .texOffs(tipTexX, tipTexY)
                            .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.5F, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(FleshMonsterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        float wiggleSpeed = 0.06F;
        float wiggleAmount = 0.35F;

        for (int i = 0; i < TENTACLE_COUNT; i++) {
            float phase = i * ((float) Math.PI * 2 / TENTACLE_COUNT);
            float sway = Mth_sin(ageInTicks * wiggleSpeed + phase) * wiggleAmount;
            tentacleBases[i].zRot += sway * 0.5F;
            tentacleBases[i].xRot += Mth_cos(ageInTicks * wiggleSpeed + phase) * (wiggleAmount * 0.4F);
            tentacleTips[i].xRot += sway;
        }

        // subtle body breathing pulse
        float breathe = Mth_sin(ageInTicks * 0.05F) * 0.03F;
        body.y += breathe * 4.0F;
    }

    private static float Mth_sin(float f) {
        return (float) Math.sin(f);
    }

    private static float Mth_cos(float f) {
        return (float) Math.cos(f);
    }
}

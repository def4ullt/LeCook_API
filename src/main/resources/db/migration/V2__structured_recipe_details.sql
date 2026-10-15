ALTER TABLE recipes ADD COLUMN servings INTEGER CHECK (servings > 0);
ALTER TABLE recipes ADD COLUMN preparation_minutes INTEGER CHECK (preparation_minutes > 0);
ALTER TABLE recipe_ingredients ADD COLUMN unit VARCHAR(16) NOT NULL DEFAULT '';
CREATE TABLE recipe_steps (
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL CHECK (step_number > 0),
    instruction TEXT NOT NULL,
    PRIMARY KEY (recipe_id, step_number)
);

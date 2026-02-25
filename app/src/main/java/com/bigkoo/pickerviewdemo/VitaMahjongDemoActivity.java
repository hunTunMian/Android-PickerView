package com.bigkoo.pickerviewdemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个轻量版的 Vita Mahjong 核心玩法 Demo：
 * 1. 只能选择“可用牌”（左右至少有一侧为空）；
 * 2. 两张相同牌可消除；
 * 3. 清空全盘即获胜。
 */
public class VitaMahjongDemoActivity extends AppCompatActivity {

    private static final int COLUMN_COUNT = 6;
    private static final int ROW_COUNT = 6;
    private static final int TILE_COUNT = COLUMN_COUNT * ROW_COUNT;

    private final String[] tilePool = new String[]{
            "🀄", "🀇", "🀈", "🀉", "🀐", "🀑", "🀒", "🀙", "🀚"
    };

    private GridLayout boardLayout;
    private TextView tvRule;
    private TextView tvScore;
    private TextView tvRemain;

    private final List<Tile> tiles = new ArrayList<>();
    private Tile selectedTile;
    private int score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vita_mahjong_demo);

        boardLayout = (GridLayout) findViewById(R.id.gl_board);
        tvRule = (TextView) findViewById(R.id.tv_rule);
        tvScore = (TextView) findViewById(R.id.tv_score);
        tvRemain = (TextView) findViewById(R.id.tv_remain);

        Button btnShuffle = (Button) findViewById(R.id.btn_shuffle);
        Button btnRestart = (Button) findViewById(R.id.btn_restart);

        tvRule.setText(getString(R.string.vita_mahjong_rule));
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shuffleRemainingTiles();
            }
        });
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initBoard();
            }
        });

        initBoard();
    }

    private void initBoard() {
        boardLayout.removeAllViews();
        tiles.clear();
        selectedTile = null;
        score = 0;

        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < TILE_COUNT / 2; i++) {
            String symbol = tilePool[i % tilePool.length];
            symbols.add(symbol);
            symbols.add(symbol);
        }
        Collections.shuffle(symbols);

        int size = getResources().getDimensionPixelSize(R.dimen.vita_tile_size);
        int margin = getResources().getDimensionPixelSize(R.dimen.vita_tile_margin);

        for (int i = 0; i < TILE_COUNT; i++) {
            final Tile tile = new Tile(i, symbols.get(i));

            TextView tileView = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            tileView.setLayoutParams(params);
            tileView.setGravity(Gravity.CENTER);
            tileView.setTextSize(20f);
            tileView.setText(tile.symbol);
            tileView.setBackgroundResource(R.drawable.bg_tile_normal);
            tileView.setTextColor(Color.parseColor("#2B2B2B"));
            tileView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTileClick(tile);
                }
            });

            tile.view = tileView;
            tiles.add(tile);
            boardLayout.addView(tileView);
        }

        updateBoardState();
    }

    private void onTileClick(Tile tile) {
        if (tile.removed) {
            return;
        }
        if (!isRemovable(tile.index)) {
            Toast.makeText(this, R.string.vita_mahjong_locked_tip, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTile == null) {
            selectedTile = tile;
            updateBoardState();
            return;
        }
        if (selectedTile == tile) {
            selectedTile = null;
            updateBoardState();
            return;
        }

        if (selectedTile.symbol.equals(tile.symbol)) {
            selectedTile.removed = true;
            tile.removed = true;
            score += 10;
            selectedTile = null;
            updateBoardState();
            if (getRemainingCount() == 0) {
                Toast.makeText(this, R.string.vita_mahjong_win_tip, Toast.LENGTH_LONG).show();
            }
        } else {
            selectedTile = tile;
            Toast.makeText(this, R.string.vita_mahjong_mismatch_tip, Toast.LENGTH_SHORT).show();
            updateBoardState();
        }
    }

    private void shuffleRemainingTiles() {
        List<String> remaining = new ArrayList<>();
        for (Tile tile : tiles) {
            if (!tile.removed) {
                remaining.add(tile.symbol);
            }
        }
        Collections.shuffle(remaining);

        int pointer = 0;
        for (Tile tile : tiles) {
            if (!tile.removed) {
                tile.symbol = remaining.get(pointer++);
            }
        }
        selectedTile = null;
        updateBoardState();
    }

    private void updateBoardState() {
        for (Tile tile : tiles) {
            if (tile.removed) {
                tile.view.setText("");
                tile.view.setBackgroundColor(Color.TRANSPARENT);
                tile.view.setEnabled(false);
                continue;
            }
            tile.view.setEnabled(true);
            tile.view.setText(tile.symbol);

            if (tile == selectedTile) {
                tile.view.setBackgroundResource(R.drawable.bg_tile_selected);
            } else if (isRemovable(tile.index)) {
                tile.view.setBackgroundResource(R.drawable.bg_tile_normal);
            } else {
                tile.view.setBackgroundResource(R.drawable.bg_tile_locked);
            }
        }

        tvScore.setText(getString(R.string.vita_mahjong_score, score));
        tvRemain.setText(getString(R.string.vita_mahjong_remain, getRemainingCount()));
    }

    private int getRemainingCount() {
        int remain = 0;
        for (Tile tile : tiles) {
            if (!tile.removed) {
                remain++;
            }
        }
        return remain;
    }

    private boolean isRemovable(int index) {
        Tile tile = tiles.get(index);
        if (tile.removed) {
            return false;
        }

        int row = index / COLUMN_COUNT;
        int col = index % COLUMN_COUNT;

        boolean leftBlocked = col > 0 && !tiles.get(row * COLUMN_COUNT + (col - 1)).removed;
        boolean rightBlocked = col < COLUMN_COUNT - 1 && !tiles.get(row * COLUMN_COUNT + (col + 1)).removed;

        return !leftBlocked || !rightBlocked;
    }

    private static class Tile {
        final int index;
        String symbol;
        boolean removed;
        TextView view;

        Tile(int index, String symbol) {
            this.index = index;
            this.symbol = symbol;
        }
    }
}
